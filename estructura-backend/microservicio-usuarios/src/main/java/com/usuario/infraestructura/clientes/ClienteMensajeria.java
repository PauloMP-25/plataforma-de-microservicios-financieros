package com.usuario.infraestructura.clientes;

import com.usuario.aplicacion.dtos.SolicitudGenerarOtp;
import java.util.UUID;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
        name = "microservicio-mensajeria",
        url = "${microservicio.mensajeria.url:http://localhost:8084}"
)
public interface ClienteMensajeria {

    /**
     * Activa la cuenta del usuario enviando el token de confirmación de email.
     * Mapea a: GET /api/v1/auth/confirmar-email?token={token}
     * @param solicitud
     */
    @PostMapping("/api/v1/mensajeria/otp/generar")
    void generarCodigo(@RequestBody SolicitudGenerarOtp solicitud);
    
    /**
     * Valida un código de recuperación y devuelve el UUID del usuario.
     * Mapea al GET que creamos en el Controlador de Mensajería.
     * @param codigo
     * @return 
     */
    @GetMapping("/api/v1/mensajeria/otp/validar-recuperacion")
    UUID validarCodigoYObtenerUsuario(@RequestParam("codigo") String codigo);
}
