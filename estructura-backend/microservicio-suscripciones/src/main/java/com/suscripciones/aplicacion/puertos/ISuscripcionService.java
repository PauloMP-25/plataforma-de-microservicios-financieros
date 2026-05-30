package com.suscripciones.aplicacion.puertos;

import com.suscripciones.aplicacion.dtos.*;
import java.util.List;
import java.util.UUID;

/**
 * Puerto de aplicación (caso de uso) para la gestión de suscripciones de usuario.
 */
public interface ISuscripcionService {

    /**
     * Crea una nueva suscripción de usuario.
     * Calcula la fecha de inicio y vencimiento inicial.
     */
    RespuestaSuscripcion crearSuscripcion(SolicitudCrearSuscripcion solicitud);

    /**
     * Busca una suscripción activa por su ID.
     */
    RespuestaSuscripcion buscarPorId(UUID id);

    /**
     * Obtiene el listado paginado y filtrado de todas las suscripciones de un usuario.
     */
    org.springframework.data.domain.Page<RespuestaSuscripcion> listarPorUsuario(
            UUID usuarioId, 
            String estado, 
            String metodoPago, 
            java.time.LocalDate fechaVencimientoAntes, 
            org.springframework.data.domain.Pageable pageable);

    /**
     * Registra manualmente el pago de una suscripción.
     * Garantiza atomicidad local e idempotencia en la base de datos.
     */
    RespuestaPagoSuscripcion registrarPagoManual(UUID id, SolicitudRegistrarPagoManual solicitud, String idempotencyKey);

    /**
     * Cancela una suscripción activa.
     */
    RespuestaSuscripcion cancelarSuscripcion(UUID id);

    /**
     * Edita los campos permitidos de una suscripción existente.
     */
    RespuestaSuscripcion editarSuscripcion(UUID id, SolicitudEditarSuscripcion solicitud);
}
