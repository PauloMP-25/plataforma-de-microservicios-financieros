package com.nucleo.financiero.infraestructura.mensajeria;

import com.libreria.comun.dtos.EventoPagoExitosoDTO;
import com.libreria.comun.mensajeria.NombresCola;
import com.nucleo.financiero.aplicacion.dtos.transacciones.SolicitudTransaccion;
import com.nucleo.financiero.aplicacion.servicios.ITransaccionService;
import com.nucleo.financiero.dominio.entidades.Categoria.TipoMovimiento;
import com.nucleo.financiero.dominio.entidades.Transaccion.MetodoPago;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Consumidor de eventos de pago para el núcleo financiero.
 * Registra automáticamente los ingresos por suscripción en el balance del usuario.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ConsumidorEventoPago {

    private final ITransaccionService servicioTransaccion;

    // TODO: Obtener este ID dinámicamente o por configuración
    private static final UUID CATEGORIA_SUSCRIPCION_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    /**
     * Al detectar un pago exitoso, lo registra como un INGRESO en el núcleo financiero.
     */
    @RabbitListener(queues = NombresCola.PAGOS_EXITOSOS)
    public void registrarIngresoSuscripcion(EventoPagoExitosoDTO evento) {
        log.info("[FINANCIERO-CONSUMER] Registrando ingreso por suscripción: {} - {}", 
            evento.planNuevo(), evento.monto());

        SolicitudTransaccion ingreso = new SolicitudTransaccion(
                evento.usuarioId(),
                "Suscripción LUKA APP",
                evento.monto(),
                TipoMovimiento.INGRESO,
                CATEGORIA_SUSCRIPCION_ID, // Categoría Genérica de Suscripciones
                MetodoPago.TRANSFERENCIA, // Stripe se considera transferencia electrónica
                "pago,suscripcion," + evento.planNuevo(),
                "Compra de suscripción mensual plan " + evento.planNuevo() + " (Stripe ID: " + evento.stripeSessionId() + ")"
        );

        try {
            servicioTransaccion.registrar(ingreso, "SYSTEM-PAGOS");
            log.info("[FINANCIERO-SUCCESS] Ingreso registrado para usuario: {}", evento.usuarioId());
        } catch (Exception e) {
            log.error("[FINANCIERO-ERROR] No se pudo registrar el ingreso: {}", e.getMessage());
            // No relanzamos para evitar bucles si la categoría no existe, 
            // pero en producción esto debería gestionarse con una DLQ.
        }
    }
}
