package com.mensajeria.infraestructura.mensajeria;

import com.mensajeria.aplicacion.dtos.AuditoriaAccesoRequestDTO; // Asegúrate de tener este DTO
import com.mensajeria.aplicacion.dtos.EstadoAcceso;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class PublicadorAuditoria {

    private final RabbitTemplate rabbitTemplate;

    private static final String EXCHANGE = "exchange.auditoria";
    // Routing key para eventos que no son estrictamente login/logout
    public static final String RK_EVENTO_SEGURIDAD = "auditoria.evento.seguridad";

    @Async
    public void publicarEvento(UUID usuario, String accion, String detalle) {
        // Reutilizamos el DTO de auditoría para mantener el contrato con el ms-auditoria
        AuditoriaAccesoRequestDTO dto = AuditoriaAccesoRequestDTO.of(
                usuario, 
                "INTERNAL", // IP interna entre microservicios
                "MS-MENSAJERIA", 
                EstadoAcceso.EXITO, 
                String.format("[%s] %s", accion, detalle), 
                LocalDateTime.now());

        try {
            rabbitTemplate.convertAndSend(EXCHANGE, RK_EVENTO_SEGURIDAD, dto);
            log.debug("[RABBITMQ] Auditoría enviada desde Mensajería: {}", accion);
        } catch (Exception ex) {
            log.error("[RABBITMQ] Fallo al enviar auditoría: {}", ex.getMessage());
        }
    }
}
