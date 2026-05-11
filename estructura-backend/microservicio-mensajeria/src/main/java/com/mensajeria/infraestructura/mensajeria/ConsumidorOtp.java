package com.mensajeria.infraestructura.mensajeria;

import com.mensajeria.aplicacion.dtos.SolicitudGenerarCodigo;
import com.mensajeria.aplicacion.servicios.IMensajeriaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class ConsumidorOtp {

    private final IMensajeriaService mensajeriaService;

    @RabbitListener(queues = ConfiguracionRabbitMQ.COLA_OTP_GENERAR)
    public void procesarSolicitudOtp(SolicitudGenerarCodigo solicitud) {
        log.info("[RABBITMQ] Solicitud de OTP recibida para usuario: {} - Propósito: {}", 
                 solicitud.usuarioId(), solicitud.proposito());
        
        try {
            // Reutilizamos tu lógica de negocio existente
            mensajeriaService.generarYEnviarCodigo(solicitud);
            log.debug("[RABBITMQ] OTP procesado y enviado con éxito.");
        } catch (Exception e) {
            log.error("[RABBITMQ] Error procesando solicitud de OTP: {}", e.getMessage());
            // Aquí RabbitMQ reintentará según la configuración o mandará a DLQ
        }
    }
}
