package com.nucleo.financiero.infraestructura.mensajeria;

import com.libreria.comun.dtos.EventoPagoExitosoDTO;
import com.libreria.comun.mensajeria.NombresCola;
import com.nucleo.financiero.aplicacion.dtos.solicitudes.SolicitudTransaccion;
import com.nucleo.financiero.aplicacion.puertos.ITransaccionService;
import com.nucleo.financiero.dominio.entidades.Categoria;
import com.nucleo.financiero.dominio.entidades.Categoria.TipoMovimiento;
import com.nucleo.financiero.dominio.entidades.Transaccion.MetodoPago;
import com.nucleo.financiero.dominio.repositorios.CategoriaRepository;
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
    private final CategoriaRepository categoriaRepository;

    /**
     * Al detectar un pago exitoso, lo registra como un INGRESO en el núcleo financiero.
     */
    @RabbitListener(queues = NombresCola.PAGOS_EXITOSOS)
    public void registrarIngresoSuscripcion(EventoPagoExitosoDTO evento) {
        log.info("[FINANCIERO-CONSUMER] Registrando ingreso por suscripción: {} - {}", 
            evento.planNuevo(), evento.monto());

        try {
            // Buscamos dinámicamente la categoría adecuada para un ingreso de suscripción.
            // Primero se intenta con "Otros Ingresos", de lo contrario se utiliza "Salario" (ambas creadas por defecto).
            Categoria categoria = categoriaRepository.findByNombreIgnoreCase("Otros Ingresos")
                    .or(() -> categoriaRepository.findByNombreIgnoreCase("Salario"))
                    .orElseThrow(() -> new IllegalStateException("No se encontró una categoría por defecto para procesar el pago."));

            UUID categoriaId = categoria.getId();

            SolicitudTransaccion ingreso = new SolicitudTransaccion(
                    evento.usuarioId(),
                    "Suscripción LUKA APP",
                    evento.monto(),
                    TipoMovimiento.INGRESO,
                    categoriaId, // Categoría recuperada dinámicamente
                    MetodoPago.TRANSFERENCIA, // Stripe se considera transferencia electrónica
                    "pago,suscripcion," + evento.planNuevo(),
                    "Compra de suscripción mensual plan " + evento.planNuevo() + " (Stripe ID: " + evento.stripeSessionId() + ")"
            );

            servicioTransaccion.registrar(ingreso, "SYSTEM-PAGOS");
            log.info("[FINANCIERO-SUCCESS] Ingreso registrado para usuario: {}", evento.usuarioId());
        } catch (Exception e) {
            log.error("[FINANCIERO-ERROR] No se pudo registrar el ingreso: {}", e.getMessage());
            // No relanzamos para evitar bucles si la categoría no existe, 
            // pero en producción esto debería gestionarse con una DLQ.
        }
    }
}
