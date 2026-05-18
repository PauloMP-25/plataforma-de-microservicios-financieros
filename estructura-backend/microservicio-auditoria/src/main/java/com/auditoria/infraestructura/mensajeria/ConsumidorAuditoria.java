package com.auditoria.infraestructura.mensajeria;

import com.auditoria.aplicacion.servicios.ServicioAuditoriaAcceso;
import com.auditoria.aplicacion.servicios.ServicioAuditoriaTransaccional;
import com.auditoria.aplicacion.servicios.ServicioRegistroAuditoria;
import com.libreria.comun.dtos.EventoAccesoDTO;
import com.libreria.comun.dtos.EventoAuditoriaDTO;
import com.libreria.comun.dtos.EventoTransaccionalDTO;
import com.libreria.comun.dtos.EventoPagoExitosoDTO;
import com.libreria.comun.mensajeria.NombresCola;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import com.rabbitmq.client.Channel;
import org.springframework.stereotype.Component;

/**
 * Consumidor de eventos de RabbitMQ para el microservicio de auditoría.
 * <p>
 * Centraliza la recepción de mensajes de seguridad y transacciones. Utiliza
 * los contratos de la librería común y delega el procesamiento a los servicios
 * especializados.
 * </p>
 * 
 * @author Paulo Moron
 * @version 2.0
 * @since 2026-05
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ConsumidorAuditoria {

    public static final String RABBIT_ERROR_HANDLER = "manejadorErroresRabbit";

    private final ServicioAuditoriaAcceso servicioAcceso;
    private final ServicioRegistroAuditoria servicioRegistro;
    private final ServicioAuditoriaTransaccional servicioTransaccional;

    /**
     * Escucha eventos de acceso (Login, Logout, Fallos) de todo el ecosistema.
     * <p>
     * Utiliza la constante {@link NombresCola#AUDITORIA_ACCESOS} y el manejador
     * de errores global para garantizar la resiliencia.
     * </p>
     * 
     * @param evento Contrato de datos de acceso de la librería común.
     * @param channel Canal RabbitMQ para confirmación manual.
     * @param deliveryTag Etiqueta identificadora del mensaje.
     */
    @RabbitListener(queues = NombresCola.AUDITORIA_ACCESOS, errorHandler = RABBIT_ERROR_HANDLER)
    public void procesarAcceso(EventoAccesoDTO evento, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws java.io.IOException {
        log.info("[RABBIT-ACCESO] Recibido evento para usuario: {}", evento.usuarioId());
        servicioAcceso.registrarAcceso(evento);
        channel.basicAck(deliveryTag, false);
    }

    /**
     * Escucha otros tipos de eventos de todo el ecosistema.
     * <p>
     * Utiliza la constante {@link NombresCola#AUDITORIA_EVENTOS} y el manejador
     * de errores global para garantizar la resiliencia.
     * </p>
     * 
     * @param evento Contrato de datos de auditoría de la librería común.
     * @param channel Canal RabbitMQ para confirmación manual.
     * @param deliveryTag Etiqueta identificadora del mensaje.
     */
    @RabbitListener(queues = NombresCola.AUDITORIA_EVENTOS, errorHandler = RABBIT_ERROR_HANDLER)
    public void procesarEvento(EventoAuditoriaDTO evento, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws java.io.IOException {
        log.info("[RABBIT-ACCESO] Recibido evento para usuario: {}", evento.usuarioId());
        servicioRegistro.registrarEvento(evento);
        channel.basicAck(deliveryTag, false);
    }

    /**
     * Escucha eventos de cambios transaccionales en las entidades de negocio.
     * 
     * @param evento Contrato de datos transaccionales de la librería común.
     * @param channel Canal RabbitMQ para confirmación manual.
     * @param deliveryTag Etiqueta identificadora del mensaje.
     */
    @RabbitListener(queues = NombresCola.AUDITORIA_TRANSACCIONES, errorHandler = RABBIT_ERROR_HANDLER)
    public void procesarTransaccion(EventoTransaccionalDTO evento, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws java.io.IOException {
        log.info("[RABBIT-TRANSAC] Registrando cambio en entidad: {} del servicio: {}",
                evento.entidadAfectada(), evento.servicioOrigen());
        servicioTransaccional.guardarEvento(evento);
        channel.basicAck(deliveryTag, false);
    }

    /**
     * Escucha eventos de pago exitoso para generar auditoría financiera.
     * Corregido para usar EventoTransaccionalDTO.crear() y "pago" por consistencia semántica.
     * 
     * @param evento Contrato de datos de pago exitoso de la librería común.
     * @param channel Canal RabbitMQ para confirmación manual.
     * @param deliveryTag Etiqueta identificadora del mensaje.
     */
    @RabbitListener(queues = NombresCola.PAGOS_EXITOSOS, errorHandler = RABBIT_ERROR_HANDLER)
    public void manejarPagoExitoso(EventoPagoExitosoDTO evento, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws java.io.IOException {
        log.info("[RABBIT-PAGOS] Registrando auditoría de pago para usuario: {}", evento.usuarioId());

        // Corregido: Usando .crear() y "pago" en lugar de "suscripcion" por consistencia semántica con pagoId
        EventoTransaccionalDTO auditoria = EventoTransaccionalDTO.crear(
                evento.usuarioId(),
                evento.pagoId(),
                "microservicio-pago",
                "pago",
                "Actualización de plan a: " + evento.planNuevo(),
                "FREE",
                evento.planNuevo());

        servicioTransaccional.guardarEvento(auditoria);
        channel.basicAck(deliveryTag, false);
    }
}