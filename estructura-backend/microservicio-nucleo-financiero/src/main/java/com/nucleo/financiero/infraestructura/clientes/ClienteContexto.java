package com.nucleo.financiero.infraestructura.clientes;

import com.nucleo.financiero.aplicacion.dtos.cliente.ContextoUsuarioDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "microservicio-cliente")
public interface ClienteContexto{

    @GetMapping("/api/v1/clientes/interno/contexto/{usuarioId}")
    ContextoUsuarioDTO obtenerContexto(@PathVariable("usuarioId") UUID usuarioId);
}
