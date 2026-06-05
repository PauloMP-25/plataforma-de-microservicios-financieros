package com.nucleo.financiero.infraestructura.mensajeria;

import com.libreria.comun.dtos.EventoPagoExitosoDTO;
import com.libreria.comun.excepciones.ExcepcionRecursoNoEncontrado;
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
     * Al detectar un pago exitoso, lo registra como un GASTO en el núcleo financiero.
     */
    @RabbitListener(queues = NombresCola.PAGOS_EXITOSOS_FINANCIERO)
    public void registrarGastoSuscripcion(EventoPagoExitosoDTO evento) {
        log.info("[FINANCIERO-CONSUMER] Registrando gasto por suscripción: {} - {}", 
            evento.planNuevo(), evento.monto());

        try {
            // Buscamos dinámicamente la categoría adecuada para un gasto de suscripción.
            Categoria categoria = categoriaRepository.findByNombreIgnoreCase("Suscripciones")
                    .orElseThrow(() -> new IllegalArgumentException("No se encontró la categoría 'Suscripciones' para procesar el pago."));

            UUID categoriaId = categoria.getId();

            SolicitudTransaccion gasto = new SolicitudTransaccion(
                    evento.usuarioId(),
                    "Suscripción LUKA APP",
                    evento.monto(),
                    TipoMovimiento.GASTO,
                    categoriaId, // Categoría recuperada dinámicamente
                    MetodoPago.TRANSFERENCIA, // Stripe se considera transferencia electrónica
                    "pago,suscripcion," + evento.planNuevo(),
                    "Compra de suscripción mensual plan " + evento.planNuevo() + " (Stripe ID: " + evento.stripeSessionId() + ")",
                    java.time.LocalDateTime.now()
            );

            servicioTransaccion.registrar(gasto, "SYSTEM-PAGOS");
            log.info("[FINANCIERO-SUCCESS] Gasto registrado para usuario: {}", evento.usuarioId());
        } catch (IllegalArgumentException | ExcepcionRecursoNoEncontrado e) {
            log.error("[FINANCIERO-ERROR] Error de negocio al procesar pago (no se relanza): {}", e.getMessage());
        } catch (Exception e) {
            log.error("[FINANCIERO-FATAL] Error de infraestructura o inesperado (se relanza a RabbitMQ/DLQ): {}", e.getMessage(), e);
            throw e;
        }
    }
}
