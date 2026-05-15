package com.mensajeria.infraestructura.clientes;

import java.util.UUID;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Feign Client para la sincronización de datos personales con el ms-usuario.
 * <p>
 * Invocado por el ms-mensajeria cuando la validación OTP por SMS en el flujo
 * de recuperación de contraseña es exitosa, garantizando que el teléfono
 * verificado quede persistido en el microservicio correspondiente.
 * </p>
 * <p>
 * Si el ms-usuario no responde, Resilience4j activa
 * {@link ClienteActualizarTelefonoFallback}, que devuelve
 * {@code "SINCRONIZACION_PENDIENTE"} sin bloquear el flujo del usuario.
 * </p>
 *
 * <p><strong>⚠ Contrato pendiente:</strong> El endpoint
 * {@code PUT /api/v1/datos-personales/telefono/{usuarioId}} debe ser creado
 * en el {@code microservicio-usuario} antes del primer despliegue conjunto.
 * Mientras no exista, el fallback capturará el 404 automáticamente.
 * Responsable: equipo de ms-usuario. Referencia: ADR-MENSAJERIA-001.</p>
 *
 * @author Paulo Moron
 * @version 1.1.0
 */
@FeignClient(
    name = "microservicio-usuario",
    url = "${microservicio.usuario.url:http://localhost:8081}",
    fallback = ClienteActualizarTelefonoFallback.class
)
public interface ClienteActualizarTelefono {

    /**
     * Actualiza el número de teléfono verificado del usuario en el ms-usuario.
     * <p>
     * Este endpoint es el contrato definido por el equipo de arquitectura para
     * la sincronización de datos personales tras la validación OTP. Si el
     * endpoint devuelve 404 (aún no implementado), el fallback lo intercepta.
     * </p>
     *
     * @param usuarioId UUID del usuario cuyo teléfono debe actualizarse; se
     *                  pasa como variable de ruta para identificación directa.
     * @param telefono  Número de teléfono en formato E.164 ({@code +51XXXXXXXXX})
     *                  verificado mediante OTP y que debe persistirse.
     * @return Cadena de confirmación del ms-usuario, o
     *         {@code "SINCRONIZACION_PENDIENTE"} si el servicio no responde.
     */
    @PutMapping("/api/v1/datos-personales/telefono/{usuarioId}")
    String sincronizarTelefono(
        @PathVariable("usuarioId") UUID usuarioId,
        @RequestParam("telefono") String telefono
    );
}
