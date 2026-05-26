package com.auditoria.infraestructura.mensajeria;

import com.auditoria.aplicacion.puertos.ServicioAuditoriaAcceso;
import com.auditoria.aplicacion.puertos.ServicioAuditoriaTransaccional;
import com.auditoria.aplicacion.puertos.ServicioRegistroAuditoria;
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
    public void procesarAcceso(EventoAccesoDTO evento) throws java.io.IOException {
        log.info("[RABBIT-ACCESO] Recibido evento para usuario: {}", evento.usuarioId());
        servicioAcceso.registrarAcceso(evento);
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
    public void procesarEvento(EventoAuditoriaDTO evento) throws java.io.IOException {
        log.info("[RABBIT-ACCESO] Recibido evento para usuario: {}", evento.usuarioId());
        servicioRegistro.registrarEvento(evento);
    }

    /**
     * Escucha eventos de cambios transaccionales en las entidades de negocio.
     * 
     * @param evento Contrato de datos transaccionales de la librería común.
     * @param channel Canal RabbitMQ para confirmación manual.
     * @param deliveryTag Etiqueta identificadora del mensaje.
     */
    @RabbitListener(queues = NombresCola.AUDITORIA_TRANSACCIONES, errorHandler = RABBIT_ERROR_HANDLER)
    public void procesarTransaccion(EventoTransaccionalDTO evento) throws java.io.IOException {
        log.info("[RABBIT-TRANSAC] Registrando cambio en entidad: {} del servicio: {}",
                evento.entidadAfectada(), evento.servicioOrigen());
        servicioTransaccional.guardarEvento(evento);
    }

    /**
     * Escucha eventos de pago exitoso para generar auditoría financiera.
     * Corregido para usar EventoTransaccionalDTO.crear() y "pago" por consistencia semántica.
     * 
     * @param evento Contrato de datos de pago exitoso de la librería común.
     * @param channel Canal RabbitMQ para confirmación manual.
     * @param deliveryTag Etiqueta identificadora del mensaje.
     */
    @RabbitListener(queues = NombresCola.PAGOS_EXITOSOS_AUDITORIA, errorHandler = RABBIT_ERROR_HANDLER)
    public void manejarPagoExitoso(EventoPagoExitosoDTO evento) throws java.io.IOException {
        log.info("[RABBIT-PAGOS] Registrando auditoría de pago para usuario: {}", evento.usuarioId());

        String planAnterior = servicioTransaccional.obtenerUltimoPlanUsuario(evento.usuarioId());

        EventoTransaccionalDTO auditoria = EventoTransaccionalDTO.crear(
                evento.usuarioId(),
                evento.pagoId(),
                "microservicio-pago",
                "pago",
                "Actualización de plan a: " + evento.planNuevo(),
                planAnterior,
                evento.planNuevo());

        servicioTransaccional.guardarEvento(auditoria);
    }
}