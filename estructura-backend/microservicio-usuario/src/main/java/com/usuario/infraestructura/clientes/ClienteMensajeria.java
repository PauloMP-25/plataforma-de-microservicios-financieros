package com.usuario.infraestructura.clientes;

import com.usuario.aplicacion.dtos.solicitudes.SolicitudGenerarOtp;
import com.usuario.aplicacion.dtos.solicitudes.SolicitudValidarRecuperacion;
import java.util.UUID;
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
        url = "${URL_PROD_MENSAJERIA:http://localhost:8084}",
        fallback = ClienteMensajeriaFallback.class
)
public interface ClienteMensajeria {

    /**
     * Activa la cuenta del usuario enviando el token de confirmación de email.
     * Mapea a: GET /api/v1/auth/confirmar-email?token={token}
     * @param solicitud
     */
    @PostMapping("/api/v1/mensajeria/otp/generar")
    UUID generarCodigo(@RequestBody SolicitudGenerarOtp solicitud);
    
    /**
     * Valida un código de recuperación y devuelve el UUID del usuario.
     * Mapea al POST que creamos en el Controlador de Mensajería.
     * @param solicitud
     * @return 
     */
    @PostMapping("/api/v1/mensajeria/otp/validar-recuperacion")
    UUID validarCodigoYObtenerUsuario(
        @RequestBody SolicitudValidarRecuperacion solicitud
    );
    
    @PostMapping("/api/v1/mensajeria/otp/validar-limite")
    void validarLimite(@RequestBody com.usuario.aplicacion.dtos.solicitudes.SolicitudVerificarLimite solicitud);
}
