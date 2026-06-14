package com.pagos.aplicacion.servicios;

import com.libreria.comun.enums.EstadoEvento;
import com.libreria.comun.excepciones.ExcepcionServicioExterno;
import com.pagos.aplicacion.enums.EstadoPago;
import com.pagos.aplicacion.puertos.IPublicadorPagos;
import com.pagos.aplicacion.puertos.IServicioWebhook;
import com.pagos.dominio.entidades.Pago;
import com.pagos.dominio.repositorios.RepositorioPago;
import com.pagos.infraestructura.mensajeria.PublicadorAuditoriaPagosImpl;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pagos.dominio.repositorios.RepositorioBoleta;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Procesador de Webhooks de Stripe con manejo de idempotencia y publicación de eventos.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ServicioWebhookImpl implements IServicioWebhook {

    private final RepositorioPago repositorioPago;
    private final RepositorioBoleta repositorioBoleta;
    private final IPublicadorPagos publicadorPagos;
    private final PublicadorAuditoriaPagosImpl publicadorAuditoria;
    private final HttpServletRequest request;

    @Override
    @Transactional
    public void procesarEvento(Event evento) {
        // IDEMPOTENCIA: Evitar procesar el mismo evento más de una vez
        if (repositorioPago.existsByStripeEventoId(evento.getId())) {
            log.info("[WEBHOOK] Evento {} ya procesado. Ignorando.", evento.getId());
            return;
        }

        log.info("[WEBHOOK] Procesando evento: {} (Tipo: {})", evento.getId(), evento.getType());

        switch (evento.getType()) {
            case "checkout.session.completed" -> procesarPagoExitoso(evento);
            case "checkout.session.expired"   -> procesarSesionExpirada(evento);
            case "checkout.session.async_payment_failed" -> procesarPagoFallido(evento);
            default -> log.debug("[WEBHOOK] Tipo de evento no manejado: {}", evento.getType());
        }
    }

    private void procesarPagoExitoso(Event evento) {
        Session sesion = (Session) evento.getDataObjectDeserializer()
            .getObject()
            .orElseGet(() -> {
                log.warn("[WEBHOOK] Versión de API Stripe distinta detectada, forzando deserialización (deserializeUnsafe)");
                try {
                    return evento.getDataObjectDeserializer().deserializeUnsafe();
                } catch (Exception e) {
                    throw new ExcepcionServicioExterno("Stripe", "Error crítico al forzar deserialización: " + e.getMessage());
                }
            });

        Pago pago = repositorioPago.findByStripeSessionId(sesion.getId())
            .orElseThrow(() -> new RuntimeException("Pago no encontrado para sesión: " + sesion.getId()));

        // Actualizar estado y vigencia
        pago.setEstado(EstadoPago.COMPLETADO);
        pago.setStripeEventoId(evento.getId());
        pago.setFechaInicioPlan(LocalDateTime.now());
        pago.setFechaFinPlan(LocalDateTime.now().plusMonths(1)); // Configuración por defecto: 1 mes
        
        repositorioPago.save(pago);

        String nombrePlan = pago.getDetalles().isEmpty() ? "N/A" : pago.getDetalles().get(0).getPlanSolicitado().name();
        log.info("[WEBHOOK] Pago COMPLETADO. Usuario: {} | Plan: {} | Pago ID: {}", pago.getUsuarioId(), nombrePlan, pago.getId());

        // Extraer el email del usuario de la sesión de Stripe (el cual fue ingresado al checkout)
        String emailUsuario = null;
        if (sesion.getCustomerDetails() != null) {
            emailUsuario = sesion.getCustomerDetails().getEmail();
        }
        if (emailUsuario == null || emailUsuario.isBlank()) {
            emailUsuario = sesion.getCustomerEmail();
        }
        if (emailUsuario == null || emailUsuario.isBlank()) {
            emailUsuario = "usuario@luka.com";
        }

        // Crear y persistir la boleta de pago en base de datos
        try {
            java.math.BigDecimal montoTotal = pago.getDetalles().isEmpty() 
                    ? java.math.BigDecimal.ZERO 
                    : pago.getDetalles().get(0).getMonto();
            
            com.pagos.dominio.entidades.Boleta boleta = com.pagos.dominio.entidades.Boleta.builder()
                    .pago(pago)
                    .codigoBoleta("BOL-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                    .emailReceptor(emailUsuario)
                    .montoTotal(montoTotal)
                    .enviadaCorreo(true)
                    .build();
            
            repositorioBoleta.save(boleta);
            log.info("[WEBHOOK] Boleta guardada exitosamente en la BD. Código: {}", boleta.getCodigoBoleta());
        } catch (Exception e) {
            log.error("[WEBHOOK-ERROR] Error al registrar la boleta en la base de datos: {}", e.getMessage(), e);
        }

        // Auditar el cambio de estado (Transaccional)
        publicadorAuditoria.auditarCambioEstadoPago(pago, "PENDIENTE");

        // Notificar al ecosistema
        publicadorPagos.publicarPagoExitoso(pago, emailUsuario);
    }

    private void procesarSesionExpirada(Event evento) {
        Session sesion = (Session) evento.getDataObjectDeserializer()
            .getObject()
            .orElseGet(() -> {
                log.warn("[WEBHOOK] Versión de API Stripe distinta detectada, forzando deserialización (deserializeUnsafe)");
                try {
                    return evento.getDataObjectDeserializer().deserializeUnsafe();
                } catch (Exception e) {
                    throw new ExcepcionServicioExterno("Stripe", "Error crítico al forzar deserialización: " + e.getMessage());
                }
            });

        repositorioPago.findByStripeSessionId(sesion.getId()).ifPresent(pago -> {
            pago.setEstado(EstadoPago.VENCIDO);
            pago.setStripeEventoId(evento.getId());
            repositorioPago.save(pago);
            log.info("[WEBHOOK] Sesión EXPIRADA para usuario: {}", pago.getUsuarioId());

            // Auditoría Transaccional y de Evento
            publicadorAuditoria.auditarCambioEstadoPago(pago, "PENDIENTE");
            publicadorAuditoria.auditarEventoSeguridad(pago.getUsuarioId(), "PAGO_EXPIRADO", "Sesión de Stripe expirada automáticamente", EstadoEvento.EXITO, request);

            publicadorPagos.publicarPagoFallido(pago);
        });
    }

    private void procesarPagoFallido(Event evento) {
        Session sesion = (Session) evento.getDataObjectDeserializer()
            .getObject()
            .orElseGet(() -> {
                log.warn("[WEBHOOK] Versión de API Stripe distinta detectada, forzando deserialización (deserializeUnsafe)");
                try {
                    return evento.getDataObjectDeserializer().deserializeUnsafe();
                } catch (Exception e) {
                    throw new ExcepcionServicioExterno("Stripe", "Error crítico al forzar deserialización: " + e.getMessage());
                }
            });

        repositorioPago.findByStripeSessionId(sesion.getId()).ifPresent(pago -> {
            pago.setEstado(EstadoPago.FALLIDO);
            pago.setStripeEventoId(evento.getId());
            repositorioPago.save(pago);
            log.warn("[WEBHOOK] Pago FALLIDO para usuario: {}", pago.getUsuarioId());

            // Auditoría Transaccional y de Evento
            publicadorAuditoria.auditarCambioEstadoPago(pago, "PENDIENTE");
            publicadorAuditoria.auditarEventoSeguridad(pago.getUsuarioId(), "PAGO_FALLIDO", "Pago rechazado o fallido en Stripe", EstadoEvento.FALLO, request);

            publicadorPagos.publicarPagoFallido(pago);
        });
    }
}
