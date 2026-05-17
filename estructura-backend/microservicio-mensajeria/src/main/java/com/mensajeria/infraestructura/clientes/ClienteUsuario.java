package com.mensajeria.infraestructura.clientes;

import java.util.UUID;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Feign Client que invoca el endpoint de activación de cuenta en el
 * Microservicio-Usuario (puerto 8081).
 *
 * Cuando la validación del OTP es exitosa, este cliente llama a
 * /api/v1/auth/confirmar-email para activar la cuenta del usuario.
 *
 * NOTA: El endpoint confirmar-email es público en el ms-usuario (ver
 * ConfiguracionSeguridad), por lo que no se requiere JWT.
 */
@FeignClient(name = "microservicio-usuario", contextId = "clienteUsuario", url = "${microservicio.usuario.url:http://localhost:8081}")
public interface ClienteUsuario {

    /**
     * Activa la cuenta del usuario por su ID. Mapea al nuevo endpoint PUT en
     * MS-Usuario.
     * 
     * @param usuarioId
     * @param telefono
     * @return
     */
    @PutMapping("/api/v1/auth/activar/{usuarioId}")
    String activarCuenta(
            @PathVariable("usuarioId") UUID usuarioId,
            @RequestParam("telefono") String telefono);
}
