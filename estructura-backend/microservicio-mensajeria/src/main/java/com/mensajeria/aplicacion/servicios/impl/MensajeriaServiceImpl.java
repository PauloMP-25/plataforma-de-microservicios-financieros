package com.mensajeria.aplicacion.servicios.impl;

import com.mensajeria.aplicacion.dtos.*;
import com.mensajeria.aplicacion.excepciones.LimiteCodigosExcedidoException;
import com.mensajeria.aplicacion.excepciones.UsuarioBloqueadoExcepcion;
import com.mensajeria.dominio.entidades.CodigoVerificacion;
import com.mensajeria.dominio.entidades.CodigoVerificacion.PropositoCodigo;
import com.mensajeria.dominio.entidades.CodigoVerificacion.TipoVerificacion;
import com.mensajeria.dominio.entidades.IntentoValidacion;
import com.mensajeria.dominio.repositorios.CodigoVerificacionRepository;
import com.mensajeria.dominio.repositorios.IntentoValidacionRepository;
import com.mensajeria.aplicacion.servicios.IMensajeriaService;
import com.mensajeria.aplicacion.servicios.IThrottlingService;
import com.mensajeria.aplicacion.servicios.NotificacionService;
import com.mensajeria.aplicacion.servicios.TipoNotificacion;
import java.util.Map;
import com.mensajeria.infraestructura.clientes.ClienteUsuario;
import com.mensajeria.infraestructura.clientes.UsuarioFeignClient;
import com.mensajeria.infraestructura.mensajeria.PublicadorAuditoria;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Implementación principal del servicio de mensajería y OTP de LUKA APP.
 * <p>
 * Orquesta la generación y validación de códigos de un solo uso, diferenciando
 * los flujos de {@code ACTIVACION_CUENTA} y {@code RESTABLECER_PASSWORD}.
 * Integra throttling por canal (Redis), auditoría (RabbitMQ) y sincronización
 * con el ms-usuario mediante Feign con fallback de Resilience4j.
 * </p>
 *
 * @author Paulo Moron
 * @version 1.1.0
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class MensajeriaServiceImpl implements IMensajeriaService {

    private final CodigoVerificacionRepository codigoRepository;
    private final IntentoValidacionRepository intentoRepository;
    private final NotificacionService notificacionService;
    private final IThrottlingService throttlingService;
    private final ClienteUsuario clienteUsuario;
    private final UsuarioFeignClient usuarioFeignClient;
    private final PublicadorAuditoria publicadorAuditoria;

    private static final SecureRandom RANDOM = new SecureRandom();

    /** Tiempo de vigencia del OTP en minutos, configurable por entorno. */
    @Value("${mensajeria.otp.expiracion-minutos:10}")
    private int expiracionMinutos;

    /** Máximo de intentos de validación fallidos antes de bloqueo temporal. */
    @Value("${mensajeria.otp.max-intentos:3}")
    private int maxIntentos;

    /** Horas de bloqueo tras exceder los intentos fallidos. */
    @Value("${mensajeria.otp.bloqueo-horas:10}")
    private long bloqueoHoras;

    // =========================================================================
    // 1. GENERACIÓN Y ENVÍO
    // =========================================================================

    /**
     * {@inheritDoc}
     * <p>
     * Flujo interno:
     * <ol>
     *   <li>Valida que el usuario no esté bloqueado por intentos fallidos.</li>
     *   <li>Valida que no haya superado el límite diario de 3 códigos.</li>
     *   <li>Registra el intento de throttling por canal en Redis.</li>
     *   <li>Genera un código OTP aleatorio de 6 dígitos y lo persiste.</li>
     *   <li>Despacha el código por EMAIL o SMS según el canal de la solicitud.</li>
     *   <li>Publica un evento de auditoría asíncrono vía RabbitMQ.</li>
     * </ol>
     * </p>
     */
    @Override
    @SuppressWarnings("null")
    @Transactional
    public RespuestaGeneracion generarYEnviarCodigo(SolicitudGenerarCodigo solicitud) {
        verificarBloqueo(solicitud.usuarioId());
        verificarLimiteDiario(solicitud.usuarioId(), solicitud.proposito());

        // Throttling por canal independiente (Redis)
        String canalThrottling = solicitud.tipo().name().toLowerCase();
        String idThrottling = (solicitud.tipo() == TipoVerificacion.EMAIL)
                ? solicitud.email()
                : solicitud.telefono();
        throttlingService.registrarIntento(canalThrottling, idThrottling);

        String codigo = String.valueOf(100_000 + RANDOM.nextInt(900_000));

        CodigoVerificacion entidad = CodigoVerificacion.builder()
                .usuarioId(solicitud.usuarioId())
                .email(solicitud.email())
                .telefono(solicitud.telefono())
                .codigo(codigo)
                .tipo(solicitud.tipo())
                .proposito(solicitud.proposito())
                .fechaExpiracion(LocalDateTime.now().plusMinutes(expiracionMinutos))
                .build();

        codigoRepository.save(entidad);

        Map<String, Object> variables = Map.of(
                "codigo", codigo,
                "proposito", solicitud.proposito()
        );

        // 2. Resolvemos el canal y el destino
        TipoNotificacion tipoEnvio = switch (solicitud.tipo()) {
            case EMAIL -> TipoNotificacion.EMAIL;
            case SMS -> TipoNotificacion.SMS;
            case WHATSAPP -> TipoNotificacion.WHATSAPP;
        };

        String destino = (solicitud.tipo() == TipoVerificacion.EMAIL) 
                ? solicitud.email() 
                : solicitud.telefono();

        // 3. Enviamos de forma agnóstica (la implementación decide si es SMTP o Twilio)
        notificacionService.enviar(tipoEnvio, destino, variables);

        String detalleAudit = String.format("OTP solicitado para %s vía %s",
                solicitud.proposito(), solicitud.tipo());

        if (solicitud.tipo() == TipoVerificacion.SMS || solicitud.tipo() == TipoVerificacion.WHATSAPP) {
            detalleAudit += " — número: " + enmascararTelefono(solicitud.telefono());

            // Si es recuperación por un canal telefónico, sincronizar con fallback de
            // Resilience4j
            if (solicitud.proposito() == PropositoCodigo.RESTABLECER_PASSWORD) {
                String resultado = usuarioFeignClient.sincronizarTelefono(
                        solicitud.usuarioId(), solicitud.telefono());
                if ("SINCRONIZACION_PENDIENTE".equals(resultado)) {
                    log.warn("[FEIGN] Sincronización de teléfono pendiente para usuario: {}",
                            solicitud.usuarioId());
                }
            }
        }

        registrarAuditoria(solicitud.usuarioId(), "OTP_SOLICITADO", detalleAudit);
        return new RespuestaGeneracion(true, "Código enviado exitosamente", solicitud.tipo().name());
    }

    // =========================================================================
    // 2. VALIDACIÓN — ACTIVACIÓN DE CUENTA
    // =========================================================================

    /**
     * {@inheritDoc}
     * <p>
     * Si el OTP es correcto, notifica al ms-usuario para activar la cuenta y
     * sincroniza el teléfono si el canal fue SMS.
     * </p>
     */
    @Override
    public RespuestaValidacion validarParaActivacion(SolicitudValidarCodigo solicitud) {
        CodigoVerificacion cv = procesarValidacionInterna(solicitud, PropositoCodigo.ACTIVACION_CUENTA);
        String telefonoVerificado = cv.getTelefono();

        log.info("[MS-MENSAJERIA] Activando cuenta para usuario: {} con teléfono: {}",
                cv.getUsuarioId(), telefonoVerificado);

        clienteUsuario.activarCuenta(cv.getUsuarioId(), telefonoVerificado);

        return new RespuestaValidacion(true, "OTP válido. Cuenta activada y teléfono sincronizado.");
    }

    // =========================================================================
    // 3. VALIDACIÓN — RECUPERACIÓN DE CONTRASEÑA
    // =========================================================================

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public UUID validarCodigoYObtenerUsuario(UUID registroId, String codigoStr) {
        CodigoVerificacion cv = codigoRepository
                .findByIdAndCodigoAndUsadoFalse(registroId, codigoStr)
                .orElseThrow(() -> new IllegalArgumentException(
                        "El código o el identificador son incorrectos."));

        if (cv.isExpirado()) {
            throw new IllegalStateException("El código ha expirado.");
        }

        cv.setUsado(true);
        cv.setFechaUso(LocalDateTime.now());
        resetearIntentos(cv.getUsuarioId());

        registrarAuditoria(cv.getUsuarioId(), "OTP_RESET_EXITOSO",
                "Validación completada para cambio de contraseña");
        return cv.getUsuarioId();
    }

    // =========================================================================
    // 4. VERIFICACIÓN ANTICIPADA DE RESTRICCIONES
    // =========================================================================

    /**
     * {@inheritDoc}
     */
    @Override
    public void verificarRestricciones(UUID usuarioId, PropositoCodigo proposito) {
        verificarBloqueo(usuarioId);
        verificarLimiteDiario(usuarioId, proposito);
        log.info("[MS-MENSAJERIA] Restricciones OK para usuario: {}", usuarioId);
    }

    // =========================================================================
    // LÓGICA PRIVADA COMPARTIDA
    // =========================================================================

    /**
     * Valida el OTP internamente para el propósito dado, registrando intentos
     * fallidos y bloqueando al usuario si supera el máximo configurado.
     *
     * @param sol  DTO con el ID del usuario y el código ingresado.
     * @param prop Propósito esperado del OTP ({@code ACTIVACION_CUENTA} o
     *             {@code RECUPERACION_PASSWORD}).
     * @return Entidad {@link CodigoVerificacion} marcada como usada y persistida.
     * @throws UsuarioBloqueadoExcepcion si el usuario ya está bloqueado.
     * @throws IllegalStateException     si no hay códigos pendientes para el
     *                                   propósito.
     * @throws IllegalArgumentException  si el código es incorrecto o expirado.
     */
    private CodigoVerificacion procesarValidacionInterna(SolicitudValidarCodigo sol, PropositoCodigo prop) {
        verificarBloqueo(sol.usuarioId());

        CodigoVerificacion cv = codigoRepository
                .findTopByUsuarioIdAndPropositoAndUsadoFalseOrderByFechaCreacionDesc(sol.usuarioId(), prop)
                .orElseThrow(() -> new IllegalStateException("No hay códigos OTP pendientes para este propósito."));

        if (!cv.esValidoPara(sol.codigo(), prop)) {
            if (registrarIntentoFallido(sol.usuarioId())) {
                throw new UsuarioBloqueadoExcepcion(sol.usuarioId(), bloqueoHoras);
            }
            throw new IllegalArgumentException("Código incorrecto o ya expirado.");
        }

        cv.setUsado(true);
        cv.setFechaUso(LocalDateTime.now());
        resetearIntentos(sol.usuarioId());
        return codigoRepository.save(cv);
    }

    /**
     * Verifica si el usuario tiene un bloqueo activo por intentos fallidos previos.
     *
     * @param uId UUID del usuario a verificar.
     * @throws UsuarioBloqueadoExcepcion si el bloqueo aún no ha expirado.
     */
    private void verificarBloqueo(UUID uId) {
        intentoRepository.findByUsuarioId(uId).ifPresent(i -> {
            if (i.isBloqueado() && !i.bloqueoExpirado()) {
                throw new UsuarioBloqueadoExcepcion(uId,
                        ChronoUnit.HOURS.between(LocalDateTime.now(), i.getBloqueadoHasta()));
            }
        });
    }

    /**
     * Incrementa el contador de intentos fallidos y bloquea al usuario si supera
     * el máximo. Emite advertencia de auditoría en el segundo intento.
     *
     * @param uId UUID del usuario que falló la validación.
     * @return {@code true} si el usuario quedó bloqueado tras este intento.
     */
    private boolean registrarIntentoFallido(UUID uId) {
        IntentoValidacion i = intentoRepository.findByUsuarioId(uId)
                .orElseGet(() -> IntentoValidacion.builder().usuarioId(uId).build());

        i.incrementarIntentos();
        int intentosActuales = i.getIntentos();

        if (intentosActuales == 2) {
            registrarAuditoria(uId, "OTP_ADVERTENCIA",
                    "Segundo intento fallido. El próximo error bloqueará la cuenta por "
                            + bloqueoHoras + " hora(s).");
        }

        if (intentosActuales >= maxIntentos) {
            i.bloquear(bloqueoHoras);
            registrarAuditoria(uId, "USUARIO_BLOQUEADO",
                    "Máximo de " + maxIntentos + " intentos alcanzado. Cuenta suspendida temporalmente.");
        }

        intentoRepository.save(i);
        return i.isBloqueado();
    }

    /**
     * Reinicia el contador de intentos fallidos del usuario tras una validación
     * exitosa, eliminando cualquier bloqueo activo.
     *
     * @param uId UUID del usuario cuyo registro de intentos debe reiniciarse.
     */
    private void resetearIntentos(UUID uId) {
        intentoRepository.findByUsuarioId(uId).ifPresent(i -> {
            i.reiniciar();
            intentoRepository.save(i);
        });
    }

    /**
     * Verifica que el usuario no haya superado el límite de 3 solicitudes diarias
     * para el propósito dado, contando desde el inicio del día actual.
     *
     * @param uId       UUID del usuario a verificar.
     * @param proposito Propósito del OTP para contar solo las solicitudes del mismo
     *                  tipo.
     * @throws LimiteCodigosExcedidoException si ya agotó los 3 intentos diarios.
     */
    private void verificarLimiteDiario(UUID uId, PropositoCodigo proposito) {
        LocalDateTime inicioDia = LocalDateTime.now().toLocalDate().atStartOfDay();
        long pedidosHoy = codigoRepository.countByUsuarioIdAndPropositoAndFechaCreacionAfter(
                uId, proposito, inicioDia);

        if (pedidosHoy >= 3) {
            log.warn("[MS-MENSAJERIA] Límite diario alcanzado — usuario: {}, propósito: {}", uId, proposito);
            throw new LimiteCodigosExcedidoException(
                    "Has alcanzado el límite de 3 solicitudes diarias para este trámite. Inténtalo mañana.");
        }
    }

    /**
     * Enmascara los últimos 4 dígitos del teléfono para logs de auditoría,
     * evitando la exposición de datos personales en trazas.
     *
     * @param tel Número de teléfono completo en formato E.164.
     * @return Cadena con los primeros dígitos reemplazados por {@code ****}.
     */
    private String enmascararTelefono(String tel) {
        if (tel == null || tel.length() < 4)
            return "****";
        return "****" + tel.substring(tel.length() - 4);
    }

    /**
     * Delega la publicación de un evento de auditoría al publicador asíncrono
     * de RabbitMQ, evitando que un fallo en la mensajería bloquee el flujo.
     *
     * @param uId    UUID del usuario que originó el evento.
     * @param accion Etiqueta corta de la acción (ej. {@code "OTP_SOLICITADO"}).
     * @param desc   Descripción detallada del evento para trazabilidad.
     */
    private void registrarAuditoria(UUID uId, String accion, String desc) {
        publicadorAuditoria.publicarEventoSeguridad(uId, accion, desc);
    }
}