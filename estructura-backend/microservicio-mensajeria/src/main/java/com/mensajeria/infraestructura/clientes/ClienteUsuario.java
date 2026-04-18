package com.mensajeria.infraestructura.clientes;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
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
@FeignClient(
        name = "microservicio-usuario",
        url = "${microservicio.usuario.url:http://localhost:8081}"
)
public interface ClienteUsuario {

    /**
     * Activa la cuenta del usuario enviando el token de confirmación de email.
     * Mapea a: GET /api/v1/auth/confirmar-email?token={token}
     *
     * @param token el valor UUID del TokenConfirmacionEmail en ms-usuario
     * @return mensaje de confirmación
     */
    @GetMapping("/api/v1/auth/confirmar-email")
    String confirmarEmail(@RequestParam("token") String token);
}
