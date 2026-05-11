package com.nucleo.financiero.infraestructura.clientes;

import com.libreria.comun.dtos.ContextoUsuarioDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

/**
 * Cliente Feign para la comunicación con el microservicio de Clientes.
 * <p>
 * Permite recuperar el contexto enriquecido del usuario (perfil, metas, límites)
 * necesario para alimentar el motor de IA.
 * </p>
 * 
 * @author Luka-Dev-Backend
 */
@FeignClient(name = "microservicio-cliente")
public interface ClienteContexto{

    /**
     * Recupera el contexto consolidado de un usuario para fines analíticos.
     * 
     * @param usuarioId Identificador único del usuario.
     * @return DTO con la información de contexto financiero y personal.
     */
    @GetMapping("/api/v1/clientes/interno/contexto/{usuarioId}")
    ContextoUsuarioDTO obtenerContexto(@PathVariable("usuarioId") UUID usuarioId);
}
