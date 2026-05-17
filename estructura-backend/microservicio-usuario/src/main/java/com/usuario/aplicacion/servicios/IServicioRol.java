package com.usuario.aplicacion.servicios;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Interfaz para la gestión de roles y planes de usuario.
 */
public interface IServicioRol {
    
    /**
     * Actualiza el plan de suscripción de un usuario y ajusta sus permisos si es necesario.
     *
     * @param usuarioId ID del usuario.
     * @param nuevoPlan Nombre del plan (ej: PREMIUM).
     * @param fechaVencimiento Fecha en la que expira el plan.
     */
    void actualizarPlanUsuario(UUID usuarioId, String nuevoPlan, LocalDateTime fechaVencimiento);
}
