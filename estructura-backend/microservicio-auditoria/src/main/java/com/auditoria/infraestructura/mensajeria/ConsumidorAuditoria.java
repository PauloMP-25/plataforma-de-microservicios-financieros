package com.auditoria.infraestructura.mensajeria;

import com.auditoria.aplicacion.servicios.ServicioAuditoriaAcceso;
import com.auditoria.aplicacion.servicios.ServicioAuditoriaTransaccional;
import com.libreria.comun.dtos.EventoAccesoDTO;
import com.libreria.comun.dtos.EventoTransaccionalDTO;
import com.auditoria.aplicacion.dtos.AuditoriaAccesoRequestDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Consumidor de eventos de RabbitMQ para el microservicio de auditoría.
 * <p>
 * Centraliza la recepción de mensajes de seguridad y transacciones, utilizando
 * el manejador de errores global de la librería común para gestionar fallos.
 * </p>
 * 
 * @author Paulo Moron
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ConsumidorAuditoria {

    private final ServicioAuditoriaAcceso servicioAcceso;
    private final ServicioAuditoriaTransaccional servicioTransaccional;

    /**
     * Escucha eventos de acceso de todo el ecosistema.
     * 
     * @param evento Datos del acceso (DTO de la librería).
     */
    @RabbitListener(
        queues = "auditoria.acceso.queue", // O usa #{NombresCola.AUDITORIA_ACCESO}
        errorHandler = "manejadorErroresRabbit" // Referencia al bean de la librería
    )
    public void procesarAcceso(EventoAccesoDTO evento) {
        log.info("[RABBIT] Procesando evento de acceso: Usuario {}", evento.usuarioId());
        
        // Adaptamos el DTO de la librería al Request local del servicio
        AuditoriaAccesoRequestDTO request = new AuditoriaAccesoRequestDTO(
            evento.usuarioId(),
            evento.ipOrigen(),
            evento.navegador(),
            evento.estado(),
            evento.detalleError(),
            evento.fecha()
        );
        
        servicioAcceso.registrarAcceso(request);
    }

    /**
     * Escucha eventos de cambios en bases de datos (transacciones).
     * 
     * @param evento Datos del cambio (DTO de la librería).
     */
    @RabbitListener(
        queues = "auditoria.transaccional.queue", 
        errorHandler = "manejadorErroresRabbit"
    )
    public void procesarTransaccion(EventoTransaccionalDTO evento) {
        log.info("[RABBIT] Registrando traza transaccional de: {}", evento.servicioOrigen());
        servicioTransaccional.guardarEvento(evento);
    }
}