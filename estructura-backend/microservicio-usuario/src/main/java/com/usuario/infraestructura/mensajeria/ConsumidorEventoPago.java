package com.usuario.infraestructura.mensajeria;

import com.libreria.comun.dtos.EventoPagoExitosoDTO;
import com.libreria.comun.mensajeria.NombresCola;
import com.usuario.aplicacion.servicios.IServicioRol;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Escucha eventos de pago exitoso provenientes de ms-pagos.
 * Actualiza el plan y la vigencia de la suscripción del usuario.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ConsumidorEventoPago {

    private final IServicioRol servicioRol;

    /**
     * Procesa el mensaje de pago exitoso.
     * RabbitMQ garantiza que si este método falla (lanza excepción), el mensaje
     * volverá a la cola o irá a la DLQ según la configuración.
     */
    @RabbitListener(queues = NombresCola.PAGOS_EXITOSOS)
    public void manejarPagoExitoso(EventoPagoExitosoDTO evento) {
        log.info("[RABBITMQ-CONSUMER] Recibido pago exitoso para usuario: {}", evento.usuarioId());
        
        try {
            servicioRol.actualizarPlanUsuario(
                evento.usuarioId(),
                evento.planNuevo(),
                evento.fechaFinPlan()
            );
        } catch (Exception e) {
            log.error("[RABBITMQ-ERROR] Error al procesar pago exitoso para usuario {}: {}", 
                evento.usuarioId(), e.getMessage());
            // Lanzamos la excepción para que RabbitMQ gestione el reintento/DLQ
            throw e;
        }
    }
}
