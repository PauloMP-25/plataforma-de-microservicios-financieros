package com.usuario.infraestructura.mensajeria;

import com.libreria.comun.dtos.EventoAccesoDTO;
import com.libreria.comun.enums.EstadoEvento;
import com.libreria.comun.mensajeria.NombresExchange;
import com.libreria.comun.mensajeria.RoutingKeys;
import com.libreria.comun.mensajeria.PublicadorEventosBase;
import com.usuario.aplicacion.dtos.SolicitudGenerarOtp;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Implementación del publicador de auditoría para el microservicio de usuario.
 * Hereda de PublicadorEventosBase para estandarizar el envío de mensajes.
 */
@Component
@Slf4j
public class PublicadorAuditoria extends PublicadorEventosBase {

    public PublicadorAuditoria(RabbitTemplate rabbitTemplate) {
        super(rabbitTemplate);
    }

    /**
     * Publica un evento de acceso utilizando el estándar de la librería común.
     * 
     * @param usuarioId ID del usuario.
     * @param ipCliente IP de origen.
     * @param estado    Estado del evento (EXITO, FALLO).
     * @param detalle   Información adicional.
     */
    public void publicarAcceso(UUID usuarioId, String ipCliente, EstadoEvento estado, String detalle) {
        EventoAccesoDTO evento = new EventoAccesoDTO(
                usuarioId,
                ipCliente,
                "LUKA-AUTH-SERVICE",
                estado,
                detalle,
                LocalDateTime.now()
        );
        
        log.info("[AUDITORIA] Publicando evento de acceso: {} para usuario: {}", estado, usuarioId);
        super.publicarAcceso(evento, estado);
    }

    /**
     * Envía una solicitud de generación de OTP con los headers necesarios para el motor de plantillas.
     * 
     * @param dto Solicitud de OTP.
     */
    public void publicarSolicitudOtp(SolicitudGenerarOtp dto) {
        log.info("[RABBITMQ] Solicitud de OTP encolada para: {} (Tipo: {})", dto.usuarioId(), dto.tipo());
        
        // Inyectamos headers para que el ms-mensajeria sepa qué plantilla usar sin abrir el JSON
        Map<String, Object> headers = Map.of(
                "x-otp-proposito", dto.proposito().name(),
                "x-otp-tipo", dto.tipo().name()
        );

        // Usamos el método de envío con headers heredado de la clase base
        super.enviarConHeaders(NombresExchange.MENSAJERIA, RoutingKeys.MENSAJERIA_OTP_GENERAR, dto, headers);
    }
}
