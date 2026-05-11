package com.auditoria.infraestructura.mensajeria;

import com.libreria.comun.mensajeria.PublicadorEventosBase;
import com.libreria.comun.dtos.EventoAccesoDTO;
import com.libreria.comun.dtos.EventoAuditoriaDTO;
import com.libreria.comun.dtos.EventoTransaccionalDTO;
import com.libreria.comun.enums.EstadoEvento;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * Publicador especializado de eventos para el Microservicio de Auditoría.
 * <p>
 * Extiende de {@link PublicadorEventosBase} para reutilizar la infraestructura
 * de mensajería asíncrona de la librería común, facilitando el reporte de accesos y trazabilidad transaccional.
 * </p>
 * 
 * @author Paulo Moron
 * @version 1.5
 * @since 2026-05
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
     * Reporta un intento de inicio de sesión de forma asíncrona.
     * <p>
     * Utiliza la lógica de enrutamiento base para dirigir el mensaje a
     * la cola de accesos con la etiqueta "exito" o "fallo", "bloqueado","logout".
     * </p>
     * 
     * @param dto Datos del evento de acceso (contrato de la librería).
     */
    public void registrarAcceso(EventoAccesoDTO dto, EstadoEvento estado) {
        this.publicarAcceso(dto, estado);
    }

    /**
     * Reporta un evento de forma asíncrona.
     * <p>
     * Utiliza la lógica de enrutamiento base para dirigir el mensaje a
     * la cola de eventos con la etiqueta "fallo" o "exito".
     * </p>
     * 
     * @param dto Datos del evento de acceso (contrato de la librería).
     */
    public void registrarEvento(EventoAuditoriaDTO dto) {
        this.publicarEvento(dto, "exito");
    }

    /**
     * Reporta la creación de un nuevo registro transaccional.
     * 
     * @param dto     Datos del cambio transaccional.
     * @param entidad Nombre de la entidad (ej: "usuario", "auditoria").
     */
    public void reportarCreacionTransaccional(EventoTransaccionalDTO dto, String entidad) {
        this.publicarTransaccion(dto, entidad);
    }
}