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
 * @version 1.6
 * @since 2026-05
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ConsumidorAuditoria {

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
     */
    @RabbitListener(queues = NombresCola.AUDITORIA_ACCESOS, errorHandler = "manejadorErroresRabbit")
    public void procesarAcceso(EventoAccesoDTO evento) {
        log.info("[RABBIT-ACCESO] Recibido evento para usuario: {}", evento.usuarioId());
        servicioAcceso.registrarAcceso(evento);
    }

    /**
     * Escucha otros tipos de eventos (Login, Logout, Fallos) de todo el ecosistema.
     * <p>
     * Utiliza la constante {@link NombresCola#AUDITORIA_ACCESOS} y el manejador
     * de errores global para garantizar la resiliencia.
     * </p>
     * 
     * @param evento Contrato de datos de acceso de la librería común.
     */
    @RabbitListener(queues = NombresCola.AUDITORIA_EVENTOS, errorHandler = "manejadorErroresRabbit")
    public void procesarEvento(EventoAuditoriaDTO evento) {
        log.info("[RABBIT-ACCESO] Recibido evento para usuario: {}", evento.usuarioId());
        servicioRegistro.registrarEvento(evento);
    }

    /**
     * Escucha eventos de cambios transaccionales en las entidades de negocio.
     * 
     * @param evento Contrato de datos transaccionales de la librería común.
     */
    @RabbitListener(queues = NombresCola.AUDITORIA_TRANSACCIONES, errorHandler = "manejadorErroresRabbit")
    public void procesarTransaccion(EventoTransaccionalDTO evento) {
        log.info("[RABBIT-TRANSAC] Registrando cambio en entidad: {} del servicio: {}",
                evento.entidadAfectada(), evento.servicioOrigen());
        servicioTransaccional.guardarEvento(evento);
    }

    /**
     * Escucha eventos de pago exitoso para generar auditoría financiera.
     * Corregido para usar EventoTransaccionalDTO.crear()
     */
    @RabbitListener(queues = NombresCola.PAGOS_EXITOSOS, errorHandler = "manejadorErroresRabbit")
    public void manejarPagoExitoso(EventoPagoExitosoDTO evento) {
        log.info("[RABBIT-PAGOS] Registrando auditoría de pago para usuario: {}", evento.usuarioId());

        // Corregido: Usando .crear() en lugar de .builder()
        EventoTransaccionalDTO auditoria = EventoTransaccionalDTO.crear(
                evento.usuarioId(),
                evento.pagoId(),
                "microservicio-pago",
                "suscripcion",
                "Actualización de plan a: " + evento.planNuevo(),
                "FREE",
                evento.planNuevo());

        servicioTransaccional.guardarEvento(auditoria);
    }
}