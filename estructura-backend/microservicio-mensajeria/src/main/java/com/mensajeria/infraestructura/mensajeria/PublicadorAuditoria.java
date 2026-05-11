package com.mensajeria.infraestructura.mensajeria;

import com.libreria.comun.dtos.EventoAuditoriaDTO;
import com.libreria.comun.mensajeria.PublicadorEventosBase;
import java.util.UUID;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * Publicador de auditoría para mensajería.
 * Hereda de la base para aprovechar el enrutamiento estándar.
 * 
 * @author Paulo Moron
 * @version 1.1.0
 */
@Component
public class PublicadorAuditoria extends PublicadorEventosBase {

    /**
     * Constructor con inyección del RabbitTemplate para la clase base.
     * 
     * @param rabbitTemplate el template de RabbitMQ
     */
    public PublicadorAuditoria(RabbitTemplate rabbitTemplate) {
        super(rabbitTemplate);
    }

    /**
     * Publica un evento de seguridad de mensajería.
     * 
     * @param usuario id del usuario
     * @param accion  acción realizada
     * @param detalle detalle adicional
     */
    public void publicarEventoSeguridad(UUID usuario, String accion, String detalle) {
        EventoAuditoriaDTO dto = EventoAuditoriaDTO.crear(
                usuario,
                accion,
                "MS-MENSAJERIA",
                "INTERNAL",
                detalle);

        super.publicarEvento(dto, ".seguridad");
    }
}
