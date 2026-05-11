package com.cliente.infraestructura.mensajeria;

import com.libreria.comun.dtos.EventoAuditoriaDTO;
import com.libreria.comun.dtos.EventoTransaccionalDTO;
import com.libreria.comun.mensajeria.PublicadorEventosBase;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * Publicador especializado de eventos para el Microservicio de Cliente.
 * <p>
 * Extiende de {@link PublicadorEventosBase} para reutilizar la infraestructura
 * de mensajería asíncrona de la librería común, facilitando el reporte de
 * accesos y trazabilidad transaccional.
 * </p>
 * <p>
 * Publica en {@code exchange.auditoria} con routing key
 * {@code cola.auditoria}. El ms-auditoria consume de esa cola y persiste
 * el evento en la tabla {@code registros_auditoria}.
 * </p>
 * 
 * @author Paulo Moron
 * @version 1.5
 * @since 2026-09
 */
@Component
public class PublicadorAuditoria extends PublicadorEventosBase {

    /**
     * Constructor que inyecta el RabbitTemplate a la clase base.
     * 
     * @param rabbitTemplate Cliente de RabbitMQ configurado.
     */
    public PublicadorAuditoria(RabbitTemplate rabbitTemplate) {
        super(rabbitTemplate);
    }

    /**
     * Reporta un evento de forma asíncrona.
     * <p>
     * Utiliza la lógica de enrutamiento base para dirigir el mensaje a
     * la cola de eventos con la etiqueta "fallo".
     * </p>
     * 
     * @param dto Datos del evento de acceso (contrato de la librería).
     */
    public void publicarEventoExitoso(EventoAuditoriaDTO dto) {
        this.publicarEvento(dto, "exito");
    }

    /**
     * Reporta un evento transaccional de forma asíncrona.
     * <p>
     * Utiliza la lógica de enrutamiento base para dirigir el mensaje a
     * la cola de eventos con la etiqueta "exito" o "fallo".
     * </p>
     * 
     * @param dto Datos del evento de acceso (contrato de la librería).
     */
    public void publicarTransaccionExitosa(EventoTransaccionalDTO dto) {
        this.publicarTransaccion(dto, dto.entidadAfectada());
    }
}
