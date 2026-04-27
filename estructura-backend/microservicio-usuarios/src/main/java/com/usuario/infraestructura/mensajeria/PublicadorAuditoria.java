package com.usuario.infraestructura.mensajeria;

import com.usuario.aplicacion.dtos.AuditoriaAccesoRequestDTO;
import com.usuario.aplicacion.dtos.EstadoAcceso;
import com.usuario.aplicacion.dtos.SolicitudGenerarOtp;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Publicador de eventos de auditoría para el ecosistema de microservicios.
 * Envía datos de acceso de forma asíncrona hacia RabbitMQ.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class PublicadorAuditoria {

    private final RabbitTemplate rabbitTemplate;
    private static final String EXCHANGE = "exchange.auditoria";
    public static final String RK_ACCESO_LOGIN = "auditoria.acceso.login";
    public static final String RK_ACCESO_FALLO = "auditoria.acceso.fallo";

    /**
     * Publica un evento de acceso al broker de mensajería.
     *
     * @param usuario ID del usuario (puede ser null).
     * @param ip Dirección de origen.
     * @param estado Enum EXITO o FALLO.
     * @param detalle Descripción del evento.
     * @param routingKey Clave de enrutamiento de RabbitMQ.
     */
    @Async
    public void publicarAcceso(UUID usuario, String ip, EstadoAcceso estado, String detalle, String routingKey) {
        AuditoriaAccesoRequestDTO dto = AuditoriaAccesoRequestDTO.of(
                usuario,
                ip,
                "Sistema-Web",
                estado,
                detalle,
                LocalDateTime.now());

        try {
            rabbitTemplate.convertAndSend(EXCHANGE, routingKey, dto);
            log.debug("[RABBITMQ] Publicado en {}: {}", routingKey, estado);
        } catch (AmqpException ex) {
            log.error("[RABBITMQ] Fallo en publicación: {}", ex.getMessage());
        }
    }

    public void publicarSolicitudOtp(SolicitudGenerarOtp dto) {
        try {
            // Enviamos el objeto DTO directamente. 
            rabbitTemplate.convertAndSend("exchange.mensajeria", "mensaje.otp.generar", dto);
            log.info("[RABBITMQ] Solicitud de OTP encolada para el usuario: {}", dto.usuarioId());
        } catch (AmqpException ex) {
            log.error("[RABBITMQ] Error al enviar solicitud de OTP: {}", ex.getMessage());
        }
    }
}
