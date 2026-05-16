package com.pagos.aplicacion.servicios.implementacion;

import com.pagos.aplicacion.dtos.RespuestaCheckoutDTO;
import com.pagos.aplicacion.dtos.RespuestaSuscripcionDTO;
import com.pagos.aplicacion.dtos.SolicitudPagoDTO;
import com.pagos.aplicacion.enums.EstadoPago;
import com.pagos.aplicacion.enums.PlanSuscripcion;
import com.pagos.aplicacion.servicios.IServicioStripe;
import com.pagos.dominio.entidades.DetallePago;
import com.pagos.dominio.entidades.Pago;
import com.pagos.dominio.repositorios.RepositorioDetallePago;
import com.pagos.dominio.repositorios.RepositorioPago;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Implementación del servicio de Stripe para la gestión de sesiones de pago.
 * Sigue el patrón de segregación de responsabilidades guardando el Pago y sus
 * Detalles por separado.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ServicioStripeImpl implements IServicioStripe {

    private final RepositorioPago repositorioPago;
    private final RepositorioDetallePago repositorioDetallePago;

    @Value("${stripe.success-url}")
    private String urlExito;

    @Value("${stripe.cancel-url}")
    private String urlCancelado;

    @Override
    @Transactional
    @SuppressWarnings("null")
    public RespuestaCheckoutDTO crearSesionCheckout(SolicitudPagoDTO solicitud, UUID usuarioId, String emailUsuario) {
        PlanSuscripcion plan = solicitud.plan();

        if (plan == PlanSuscripcion.FREE) {
            throw new IllegalArgumentException("El plan FREE no requiere un proceso de pago.");
        }

        // Convertir a centavos para Stripe (BigDecimal -> long)
        long montoCentavos = plan.getPrecio()
                .multiply(new BigDecimal("100"))
                .longValueExact();

        try {
            SessionCreateParams params = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.PAYMENT)
                    .setSuccessUrl(urlExito + "?session_id={CHECKOUT_SESSION_ID}")
                    .setCancelUrl(urlCancelado)
                    .setCustomerEmail(emailUsuario)
                    .putMetadata("usuarioId", usuarioId.toString())
                    .addLineItem(SessionCreateParams.LineItem.builder()
                            .setQuantity(1L)
                            .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                                    .setCurrency("pen")
                                    .setUnitAmount(montoCentavos)
                                    .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                            .setName("Suscripción " + plan.name() + " — LUKA APP")
                                            .setDescription(plan.getDescripcion())
                                            .build())
                                    .build())
                            .build())
                    .build();

            Session sesion = Session.create(params);

            // 1. Persistir el encabezado del pago
            Pago pago = Pago.builder()
                    .usuarioId(usuarioId)
                    .estado(EstadoPago.PENDIENTE)
                    .stripeSessionId(sesion.getId())
                    .build();

            Pago pagoGuardado = repositorioPago.save(pago);

            // 2. Persistir el detalle del pago (Plan, monto, etc.)
            DetallePago detalle = DetallePago.builder()
                    .pago(pagoGuardado)
                    .planSolicitado(plan)
                    .monto(plan.getPrecio())
                    .moneda("PEN")
                    .descripcion("Suscripción al plan " + plan.name())
                    .cantidad(1)
                    .descuento(BigDecimal.ZERO)
                    .build();

            repositorioDetallePago.save(detalle);

            log.info("[STRIPE] Sesión de Checkout creada: {} para usuario: {}", sesion.getId(), usuarioId);

            return new RespuestaCheckoutDTO(
                    pagoGuardado.getId().toString(),
                    sesion.getUrl(),
                    plan.name(),
                    plan.getPrecio(),
                    "PEN");

        } catch (StripeException e) {
            log.error("[STRIPE-ERROR] Fallo al crear sesión: {}", e.getMessage());
            throw new RuntimeException("Error en la comunicación con la pasarela de pagos.", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public RespuestaSuscripcionDTO obtenerEstadoSuscripcion(UUID usuarioId) {
        // Buscamos el último pago completado para determinar el plan activo
        return repositorioPago.findFirstByUsuarioIdAndEstadoOrderByFechaCreacionDesc(usuarioId, EstadoPago.COMPLETADO)
                .map(pago -> {
                    // Obtenemos el detalle (usamos el primero ya que por ahora solo hay uno por
                    // pago)
                    DetallePago detalle = pago.getDetalles().get(0);

                    boolean activo = pago.getFechaFinPlan() != null &&
                            pago.getFechaFinPlan().isAfter(LocalDateTime.now());

                    return new RespuestaSuscripcionDTO(
                            detalle.getPlanSolicitado(),
                            pago.getEstado(),
                            detalle.getMonto(),
                            detalle.getMoneda(),
                            pago.getFechaFinPlan(),
                            activo);
                })
                .orElse(new RespuestaSuscripcionDTO(
                        PlanSuscripcion.FREE,
                        EstadoPago.COMPLETADO,
                        BigDecimal.ZERO,
                        "PEN",
                        null,
                        true));
    }
}
