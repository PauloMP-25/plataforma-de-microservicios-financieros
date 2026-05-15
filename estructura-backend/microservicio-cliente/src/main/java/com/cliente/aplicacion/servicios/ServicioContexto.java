package com.cliente.aplicacion.servicios;


import com.libreria.comun.dtos.ContextoEstrategicoIADTO;
import java.util.UUID;

/**
 * Interfaz de servicio para compartir los datos personales y financieros del
 * cliente.
 * <p>
 * Se encarga la agrupación de los datos personales y financieros del cliente
 * para que el microservicio-ia genere un consejo personalizado.
 * </p>
 *
 * @author Paulo Moron
 * @since 2026-05
 */
public interface ServicioContexto {

    /**
     * Retorna un contexto financiero ligero, estrictamente necesario para que
     * la IA evalúe la situación financiera del cliente.
     * 
     * @param usuarioId ID del usuario
     * @return DTO optimizado con los datos necesarios para el modelo de IA.
     */
    ContextoEstrategicoIADTO obtenerContextoFinanciero(UUID usuarioId);

    /**
     * Retorna el contexto completo consolidado del usuario (Perfil, Metas, Límites).
     * Utilizado por el ms-nucleo-financiero para enriquecer solicitudes de IA.
     * 
     * @param usuarioId ID del usuario
     * @return DTO con toda la información de contexto.
     */
    com.libreria.comun.dtos.ContextoUsuarioDTO obtenerContextoCompleto(UUID usuarioId);

    /**
     * Refresca asíncronamente el contexto de la IA en Redis.
     * 
     * @param usuarioId ID del usuario
     */
    void refrescarContextoRedis(UUID usuarioId);
}
