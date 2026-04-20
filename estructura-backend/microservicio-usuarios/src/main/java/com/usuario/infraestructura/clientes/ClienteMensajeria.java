package com.usuario.infraestructura.clientes;

import com.usuario.aplicacion.dtos.SolicitudGenerarOtp;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

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
        name = "microservicio-mensajeria",
        url = "${microservicio.mensajeria.url:http://localhost:8084}"
)
public interface ClienteMensajeria {

    /**
     * Activa la cuenta del usuario enviando el token de confirmación de email.
     * Mapea a: GET /api/v1/auth/confirmar-email?token={token}
     *
     * @param solicitud
     */
    @PostMapping("/api/v1/mensajeria/otp/generar")
    void generarCodigo(@RequestBody SolicitudGenerarOtp solicitud);
}
