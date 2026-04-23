package com.usuario.infraestructura.mensajeria;

import com.usuario.aplicacion.dtos.AuditoriaAccesoRequestDTO;
import com.usuario.aplicacion.dtos.EstadoAcceso;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Publicador de eventos de auditoría hacia RabbitMQ.
 *
 * Este componente es utilizado por los OTROS microservicios del ecosistema
 * cuando necesitan reportar eventos de seguridad o cambios de datos al
 * microservicio de auditoría de forma desacoplada y asíncrona.
 *
 * USO TÍPICO: - Microservicio-Usuario → reportar login/logout via
 * publicarAcceso() - Microservicio-Financiero → reportar cambios de
 * transacciones via publicarCambio() - API Gateway → reportar intentos
 * bloqueados via publicarAcceso()
 *
 * Routing Keys usadas: "auditoria.acceso.login" → cola.auditoria.accesos
 * "auditoria.acceso.logout" → cola.auditoria.accesos
 * "auditoria.transaccion.creacion" → cola.auditoria.transacciones
 * "auditoria.transaccion.actualizacion" → cola.auditoria.transacciones
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class PublicadorAuditoria {

    private final RabbitTemplate rabbitTemplate;

    // Nombres directos (puedes usar variables de entorno si prefieres)
    private static final String EXCHANGE = "exchange.auditoria";
    public static final String RK_ACCESO_LOGIN = "auditoria.acceso.login";
    public static final String RK_ACCESO_FALLO = "auditoria.acceso.fallo";

    @Async
    public void publicarAcceso(UUID usuario, String ip, String estado, String detalle, String routingKey) {
        // Construimos el DTO aquí mismo para que el Servicio esté limpio
        AuditoriaAccesoRequestDTO dto = AuditoriaAccesoRequestDTO.of(
                usuario, 
                ip, 
                "Sistema-Web", 
                estado.equals("EXITO") ? EstadoAcceso.EXITO : EstadoAcceso.FALLO, 
                detalle, 
                LocalDateTime.now());

        try {
            rabbitTemplate.convertAndSend(EXCHANGE, routingKey, dto);
            log.debug("[RABBITMQ] Evento enviado: {}", routingKey);
        } catch (AmqpException ex) {
            log.error("[RABBITMQ] Error al enviar: {}", ex.getMessage());
        }
    }
}
