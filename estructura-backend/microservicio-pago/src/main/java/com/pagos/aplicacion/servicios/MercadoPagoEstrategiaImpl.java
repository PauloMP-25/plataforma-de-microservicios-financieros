package com.pagos.aplicacion.servicios;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.libreria.comun.dtos.EventoPagoExitosoDTO;
import com.libreria.comun.enums.EstadoEvento;
import com.mercadopago.client.preapproval.PreapprovalClient;
import com.mercadopago.client.preapproval.PreapprovalCreateRequest;
import com.mercadopago.client.preapproval.PreApprovalAutoRecurringCreateRequest;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import com.mercadopago.resources.preapproval.Preapproval;
import com.pagos.aplicacion.dtos.RespuestaCheckoutDTO;
import com.pagos.aplicacion.dtos.RespuestaSuscripcionDTO;
import com.pagos.aplicacion.dtos.SolicitudPagoDTO;
import com.pagos.aplicacion.enums.EstadoPago;
import com.pagos.aplicacion.enums.PlanSuscripcion;
import com.pagos.aplicacion.enums.ProveedorPago;
import com.pagos.aplicacion.puertos.IPasarelaPagoEstrategia;
import com.pagos.aplicacion.puertos.IPublicadorPagos;
import com.pagos.dominio.entidades.Boleta;
import com.pagos.dominio.entidades.DetallePago;
import com.pagos.dominio.entidades.Pago;
import com.pagos.dominio.repositorios.RepositorioBoleta;
import com.pagos.dominio.repositorios.RepositorioDetallePago;
import com.pagos.dominio.repositorios.RepositorioPago;
import com.pagos.infraestructura.configuracion.PropiedadesMercadoPago;
import com.pagos.infraestructura.excepciones.ExcepcionFirmaWebhookInvalida;
import com.pagos.infraestructura.excepciones.ExcepcionMercadoPago;
import com.pagos.infraestructura.mensajeria.PublicadorAuditoriaPagosImpl;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.UUID;

/**
 * Implementación de la estrategia de pagos para Mercado Pago (Adapter + Strategy Pattern).
 *
 * <p>Actúa como Adapter entre la API de Mercado Pago y el dominio interno de LUKA APP,
 * homogeneizando el flujo de suscripción recurrente (Preapproval) con el flujo existente
 * de Stripe para el bus de mensajes.</p>
 *
 * <p>Aplica el patrón Transactional Outbox para garantizar la entrega del evento
 * {@code EventoPagoExitosoDTO} a RabbitMQ incluso ante fallos de red.</p>
 *
 * <p>Soporte de planes: {@link PlanSuscripcion#PRO} (S/15.00) y
 * {@link PlanSuscripcion#PREMIUM} (S/25.00), moneda PEN (Perú).</p>
 *
 * @author LUKA APP Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MercadoPagoEstrategiaImpl implements IPasarelaPagoEstrategia {

    private static final String ALGORITMO_HMAC = "HmacSHA256";
    private static final String MONEDA_PEN = "PEN";

    private final RepositorioPago repositorioPago;
    private final RepositorioDetallePago repositorioDetallePago;
    private final RepositorioBoleta repositorioBoleta;
    private final PropiedadesMercadoPago propiedades;
    private final IPublicadorPagos publicadorPagos;
    private final PublicadorAuditoriaPagosImpl publicadorAuditoria;
    private final ObjectMapper objectMapper;

    // ─────────────────────────────────────────────────────────────────────────
    // IPasarelaPagoEstrategia
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * {@inheritDoc}
     * Identifica esta estrategia como proveedor MERCADOPAGO.
     */
    @Override
    public ProveedorPago getProveedor() {
        return ProveedorPago.MERCADOPAGO;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Crea un Preapproval en Mercado Pago para el plan solicitado (PRO o PREMIUM).
     * Persiste el Pago en estado {@code PENDIENTE} con el preapproval_id como referencia
     * y retorna el {@code init_point} para redirigir al usuario.</p>
     *
     * @throws ExcepcionMercadoPago si la API de Mercado Pago devuelve un error.
     * @throws IllegalArgumentException si el plan solicitado es FREE.
     */
    @Override
    @Transactional
    public RespuestaCheckoutDTO crearSesionCheckout(
            SolicitudPagoDTO solicitud, UUID usuarioId, String emailUsuario) {

        PlanSuscripcion plan = solicitud.plan();

        if (plan == PlanSuscripcion.FREE) {
            throw new IllegalArgumentException("El plan FREE no requiere un proceso de pago.");
        }

        try {
            // 1. Construir el request de Preapproval (débito recurrente mensual)
            PreApprovalAutoRecurringCreateRequest autoRecurring = PreApprovalAutoRecurringCreateRequest.builder()
                    .frequency(1)
                    .frequencyType("months")
                    .transactionAmount(plan.getPrecio())
                    .currencyId(MONEDA_PEN)
                    .build();

            PreapprovalCreateRequest request = PreapprovalCreateRequest.builder()
                    .payerEmail(emailUsuario)
                    .backUrl(propiedades.getSuccessUrl())
                    .reason("Suscripción Luka App " + plan.name())
                    .autoRecurring(autoRecurring)
                    .status("pending")
                    .externalReference(usuarioId.toString())  // Clave de idempotencia y trazabilidad
                    .build();

            // 2. Llamar a la API de Mercado Pago
            PreapprovalClient client = new PreapprovalClient();
            Preapproval preapproval = client.create(request);

            log.info("[MERCADOPAGO] Preapproval creado: {} para usuario: {}", preapproval.getId(), usuarioId);

            // 3. Persistir la cabecera del pago con referencia al preapproval_id
            Pago pago = Pago.builder()
                    .usuarioId(usuarioId)
                    .estado(EstadoPago.PENDIENTE)
                    .stripeSessionId(preapproval.getId())  // Campo genérico: almacena preapproval_id
                    .build();

            Pago pagoGuardado = repositorioPago.save(pago);

            // 4. Persistir el detalle del pago
            DetallePago detalle = DetallePago.builder()
                    .pago(pagoGuardado)
                    .planSolicitado(plan)
                    .monto(plan.getPrecio())
                    .moneda(MONEDA_PEN)
                    .descripcion("Suscripción recurrente al plan " + plan.name() + " — Mercado Pago")
                    .cantidad(1)
                    .descuento(BigDecimal.ZERO)
                    .build();

            repositorioDetallePago.save(detalle);

            // 5. Retornar la URL de checkout (init_point) al frontend
            return new RespuestaCheckoutDTO(
                    pagoGuardado.getId().toString(),
                    preapproval.getInitPoint(),
                    plan.name(),
                    plan.getPrecio().setScale(2, RoundingMode.HALF_UP),
                    MONEDA_PEN);

        } catch (MPApiException e) {
            log.error("[MERCADOPAGO-API-ERROR] Status: {} | Respuesta: {}",
                    e.getApiResponse().getStatusCode(), e.getMessage());
            throw new ExcepcionMercadoPago("crearPreapproval",
                    "HTTP " + e.getApiResponse().getStatusCode() + " — " + e.getMessage());
        } catch (MPException e) {
            log.error("[MERCADOPAGO-SDK-ERROR] {}", e.getMessage());
            throw new ExcepcionMercadoPago("crearPreapproval", e.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     * Consulta el último pago completado del usuario para determinar su plan activo.
     * Reutiliza la misma lógica agnóstica del repositorio, sin distinción de pasarela.
     */
    @Override
    @Transactional(readOnly = true)
    public RespuestaSuscripcionDTO obtenerEstadoSuscripcion(UUID usuarioId) {
        return repositorioPago
                .findFirstByUsuarioIdAndEstadoOrderByFechaCreacionDesc(usuarioId, EstadoPago.COMPLETADO)
                .map(pago -> {
                    DetallePago detalle = pago.getDetalles().get(0);
                    boolean activo = pago.getFechaFinPlan() != null &&
                            pago.getFechaFinPlan().isAfter(LocalDateTime.now());

                    return new RespuestaSuscripcionDTO(
                            detalle.getPlanSolicitado(),
                            pago.getEstado(),
                            detalle.getMonto().setScale(2, RoundingMode.HALF_UP),
                            detalle.getMoneda(),
                            pago.getFechaFinPlan(),
                            activo);
                })
                .orElse(new RespuestaSuscripcionDTO(
                        PlanSuscripcion.FREE,
                        EstadoPago.COMPLETADO,
                        BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                        MONEDA_PEN,
                        null,
                        true));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Procesamiento de Webhooks / IPN
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Valida la firma HMAC-SHA256 del webhook de Mercado Pago y procesa la notificación.
     *
     * <p>Flujo de seguridad:</p>
     * <ol>
     *   <li>Extrae el timestamp ({@code ts}) y hash ({@code v1}) del header {@code x-signature}.</li>
     *   <li>Construye el template: {@code id:{requestId};request-date:{ts};body:{payload};}.</li>
     *   <li>Calcula HMAC-SHA256 con el webhook secret configurado.</li>
     *   <li>Compara con {@code MessageDigest.isEqual} (timing-safe) para prevenir timing attacks.</li>
     *   <li>Si la firma es válida, consulta el estado actualizado del preapproval en la API de MP.</li>
     * </ol>
     *
     * @param payloadCrudo Cuerpo crudo del POST de Mercado Pago.
     * @param firma        Valor del header {@code x-signature}.
     * @param requestId    Valor del header {@code x-request-id}.
     * @param request      Petición HTTP (para auditoría).
     * @throws ExcepcionFirmaWebhookInvalida si la firma no coincide.
     * @throws ExcepcionMercadoPago          si falla la consulta a la API de MP.
     */
    @Transactional
    public void procesarNotificacion(String payloadCrudo, String firma,
                                     String requestId, HttpServletRequest request) {
        // ── 1. Validar firma HMAC-SHA256 ──────────────────────────────────────
        validarFirmaHmac(payloadCrudo, firma, requestId);

        // ── 2. Parsear el ID del recurso desde el payload ─────────────────────
        String preapprovalId = extraerPreapprovalId(payloadCrudo);
        String tipoNotificacion = extraerTipoNotificacion(payloadCrudo);

        if (preapprovalId == null || !tipoNotificacion.contains("preapproval")) {
            log.debug("[WEBHOOK-MP] Notificación de tipo '{}' ignorada (no es preapproval).", tipoNotificacion);
            return;
        }

        // ── 3. Verificar idempotencia ─────────────────────────────────────────
        if (repositorioPago.existsByStripeEventoId(requestId)) {
            log.info("[WEBHOOK-MP] Notificación requestId={} ya procesada. Ignorando duplicado.", requestId);
            return;
        }

        // ── 4. Consultar estado actualizado del preapproval en Mercado Pago ───
        try {
            PreapprovalClient client = new PreapprovalClient();
            Preapproval preapproval = client.get(preapprovalId);
            String status = preapproval.getStatus();

            log.info("[WEBHOOK-MP] Estado del preapproval {}: {}", preapprovalId, status);

            switch (status) {
                case "authorized" -> procesarPagoAutorizado(preapproval, requestId, request);
                case "paused", "cancelled" -> procesarSuscripcionSuspendida(preapproval, requestId);
                default -> log.debug("[WEBHOOK-MP] Estado '{}' no requiere acción.", status);
            }

        } catch (MPApiException e) {
            log.error("[WEBHOOK-MP-ERROR] Error al consultar preapproval {}: {}", preapprovalId, e.getMessage());
            throw new ExcepcionMercadoPago("consultarPreapproval",
                    "HTTP " + e.getApiResponse().getStatusCode() + " — " + e.getMessage());
        } catch (MPException e) {
            log.error("[WEBHOOK-MP-SDK-ERROR] {}", e.getMessage());
            throw new ExcepcionMercadoPago("consultarPreapproval", e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Métodos privados de negocio
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Procesa un preapproval autorizado: actualiza el pago a COMPLETADO, genera la boleta
     * (reutilizando el patrón de ServicioWebhookImpl) y publica el evento unificado a RabbitMQ.
     */
    private void procesarPagoAutorizado(Preapproval preapproval, String requestId,
                                        HttpServletRequest request) {
        Pago pago = repositorioPago.findByStripeSessionId(preapproval.getId())
                .orElseThrow(() -> new RuntimeException(
                        "[WEBHOOK-MP] No se encontró el Pago para preapproval_id: " + preapproval.getId()));

        // Actualizar estado del pago
        pago.setEstado(EstadoPago.COMPLETADO);
        pago.setStripeEventoId(requestId);
        pago.setFechaInicioPlan(LocalDateTime.now());
        pago.setFechaFinPlan(LocalDateTime.now().plusMonths(1));
        repositorioPago.save(pago);

        String emailUsuario = preapproval.getPayerEmail() != null
                ? preapproval.getPayerEmail()
                : "usuario@luka.com";
        String nombrePlan = pago.getDetalles().isEmpty()
                ? "N/A"
                : pago.getDetalles().get(0).getPlanSolicitado().name();

        log.info("[WEBHOOK-MP] Pago AUTORIZADO. Usuario: {} | Plan: {} | Pago ID: {}",
                pago.getUsuarioId(), nombrePlan, pago.getId());

        // ── Generar y persistir la Boleta (reutilización del patrón existente) ─
        try {
            BigDecimal montoTotal = pago.getDetalles().isEmpty()
                    ? BigDecimal.ZERO
                    : pago.getDetalles().get(0).getMonto();

            Boleta boleta = Boleta.builder()
                    .pago(pago)
                    .codigoBoleta("BOL-MP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                    .emailReceptor(emailUsuario)
                    .montoTotal(montoTotal)
                    .enviadaCorreo(true)
                    .build();

            repositorioBoleta.save(boleta);
            log.info("[WEBHOOK-MP] Boleta MP guardada exitosamente. Código: {}", boleta.getCodigoBoleta());

        } catch (Exception e) {
            log.error("[WEBHOOK-MP-ERROR] Error al generar boleta MP: {}", e.getMessage(), e);
        }

        // ── Auditar el cambio de estado ───────────────────────────────────────
        publicadorAuditoria.auditarCambioEstadoPago(pago, "PENDIENTE");
        publicadorAuditoria.auditarEventoSeguridad(
                pago.getUsuarioId(), "MERCADOPAGO_AUTORIZADO",
                "Suscripción recurrente autorizada via Mercado Pago. Plan: " + nombrePlan,
                EstadoEvento.EXITO, request);

        // ── Publicar EventoPagoExitosoDTO al bus de mensajes (Outbox Pattern) ─
        publicadorPagos.publicarPagoExitoso(pago, emailUsuario);
    }

    /**
     * Procesa una suscripción pausada o cancelada: actualiza el pago a FALLIDO
     * y notifica al ecosistema.
     */
    private void procesarSuscripcionSuspendida(Preapproval preapproval, String requestId) {
        repositorioPago.findByStripeSessionId(preapproval.getId()).ifPresent(pago -> {
            pago.setEstado(EstadoPago.FALLIDO);
            pago.setStripeEventoId(requestId);
            repositorioPago.save(pago);

            log.warn("[WEBHOOK-MP] Suscripción SUSPENDIDA/CANCELADA para usuario: {} | Status: {}",
                    pago.getUsuarioId(), preapproval.getStatus());

            publicadorAuditoria.auditarCambioEstadoPago(pago, "COMPLETADO");
            publicadorPagos.publicarPagoFallido(pago);
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Métodos privados de seguridad
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Valida la firma HMAC-SHA256 del webhook de Mercado Pago.
     * Usa {@code MessageDigest.isEqual} para comparación timing-safe y prevenir timing attacks.
     *
     * <p>Formato del header {@code x-signature}: {@code ts=1234567890,v1=sha256hashhere}</p>
     *
     * @param payload   Cuerpo crudo de la petición HTTP.
     * @param firma     Valor del header {@code x-signature}.
     * @param requestId Valor del header {@code x-request-id}.
     * @throws ExcepcionFirmaWebhookInvalida si la firma no coincide.
     */
    private void validarFirmaHmac(String payload, String firma, String requestId) {
        try {
            // Parsear el header x-signature: "ts=1234567890,v1=abc..."
            String ts = null;
            String v1 = null;
            for (String parte : firma.split(",")) {
                String[] kv = parte.split("=", 2);
                if (kv.length == 2) {
                    if ("ts".equals(kv[0].trim())) ts = kv[1].trim();
                    if ("v1".equals(kv[0].trim())) v1 = kv[1].trim();
                }
            }

            if (ts == null || v1 == null) {
                throw new ExcepcionFirmaWebhookInvalida("Formato de header x-signature inválido.");
            }

            // Construir el template de firma según la documentación oficial de Mercado Pago
            String template = "id:" + requestId + ";request-date:" + ts + ";body:" + payload + ";";

            // Calcular HMAC-SHA256 con el webhook secret
            Mac mac = Mac.getInstance(ALGORITMO_HMAC);
            SecretKeySpec secretKey = new SecretKeySpec(
                    propiedades.getWebhookSecret().getBytes(StandardCharsets.UTF_8), ALGORITMO_HMAC);
            mac.init(secretKey);
            byte[] hashCalculado = mac.doFinal(template.getBytes(StandardCharsets.UTF_8));

            // Comparación timing-safe
            byte[] hashRecibido = HexFormat.of().parseHex(v1);
            if (!MessageDigest.isEqual(hashCalculado, hashRecibido)) {
                throw new ExcepcionFirmaWebhookInvalida();
            }

            log.debug("[WEBHOOK-MP-SEGURIDAD] Firma HMAC-SHA256 validada correctamente.");

        } catch (ExcepcionFirmaWebhookInvalida e) {
            throw e;
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("[WEBHOOK-MP-SEGURIDAD] Error interno al validar firma: {}", e.getMessage());
            throw new ExcepcionFirmaWebhookInvalida("Error interno de criptografía: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("[WEBHOOK-MP-SEGURIDAD] Hash v1 con formato hexadecimal inválido: {}", e.getMessage());
            throw new ExcepcionFirmaWebhookInvalida("Hash recibido no es hexadecimal válido.");
        }
    }

    /** Extrae el preapproval_id del payload JSON de la notificación. */
    private String extraerPreapprovalId(String payloadCrudo) {
        try {
            JsonNode root = objectMapper.readTree(payloadCrudo);
            JsonNode dataNode = root.path("data").path("id");
            return dataNode.isMissingNode() ? null : dataNode.asText();
        } catch (Exception e) {
            log.warn("[WEBHOOK-MP] No se pudo extraer el ID del payload: {}", e.getMessage());
            return null;
        }
    }

    /** Extrae el tipo de recurso del payload JSON de la notificación (ej: "preapproval"). */
    private String extraerTipoNotificacion(String payloadCrudo) {
        try {
            JsonNode root = objectMapper.readTree(payloadCrudo);
            return root.path("type").asText("");
        } catch (Exception e) {
            return "";
        }
    }
}
