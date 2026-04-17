package com.mensajeria.presentacion.controladores;

import com.mensajeria.aplicacion.dtos.MensajeriaDtos.*;
import com.mensajeria.aplicacion.excepciones.UsuarioBloqueadoExcepcion;
import com.mensajeria.aplicacion.servicios.ServicioMensajeria;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Controlador REST del Microservicio-Mensajería.
 *
 * Endpoints expuestos: POST /api/v1/mensajeria/otp/generar → genera y envía un
 * código OTP POST /api/v1/mensajeria/otp/validar → valida el código ingresado
 * por el usuario
 */
@RestController
@RequestMapping("/api/v1/mensajeria/otp")
@RequiredArgsConstructor
@Slf4j
public class ControladorMensajeria {

    private final ServicioMensajeria servicioMensajeria;

    // =========================================================================
    // POST /api/v1/mensajeria/otp/generar
    // =========================================================================
    /**
     * Genera un código OTP de 6 dígitos y lo envía por EMAIL o PHONE.
     *
     * Body esperado: { "usuarioId" : "550e8400-e29b-41d4-a716-446655440000",
     * "email" : "usuario@ejemplo.com", "telefono" : "+51999888777", // solo si
     * tipo = PHONE "tipo" : "EMAIL" // o "PHONE" }
     * @param solicitud
     * @return 
     */
    @PostMapping("/generar")
    public ResponseEntity<?> generarCodigo(
            @Valid @RequestBody SolicitudGenerarCodigo solicitud) {

        log.debug("POST /otp/generar — usuarioId: {}, tipo: {}",
                solicitud.usuarioId(), solicitud.tipo());

        try {
            RespuestaGeneracion respuesta = servicioMensajeria.generarYEnviarCodigo(solicitud);
            return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);

        } catch (UsuarioBloqueadoExcepcion ex) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(errorBody(429, "USUARIO_BLOQUEADO", ex.getMessage()));

        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest()
                    .body(errorBody(400, "SOLICITUD_INVALIDA", ex.getMessage()));

        } catch (RuntimeException ex) {
            log.error("Error generando OTP para usuarioId {}: {}",
                    solicitud.usuarioId(), ex.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorBody(500, "ERROR_ENVIO",
                            "No se pudo enviar el código. Intente nuevamente."));
        }
    }

    // =========================================================================
    // POST /api/v1/mensajeria/otp/validar
    // =========================================================================
    /**
     * Valida el código OTP ingresado por el usuario. Si es correcto, activa la
     * cuenta en el Microservicio-Usuario vía Feign.
     *
     * Body esperado: { "usuarioId" : "550e8400-e29b-41d4-a716-446655440000",
     * "codigo" : "482910", "tokenActivacion" :
     * "uuid-del-token-confirmacion-email" // de ms-usuario }
     *
     * El campo tokenActivacion es el valor generado por el
     * Microservicio-Usuario al momento del registro
     * (TokenConfirmacionEmail.token). El frontend lo recibe opcionalmente o el
     * ms-usuario lo puede pasar al llamar a este endpoint.
     * @param solicitud
     * @param tokenActivacion
     * @return 
     */
    @PostMapping("/validar")
    public ResponseEntity<?> validarCodigo(
            @Valid @RequestBody SolicitudValidarCodigo solicitud,
            @RequestParam(required = false) String tokenActivacion) {

        log.debug("POST /otp/validar — usuarioId: {}", solicitud.usuarioId());

        try {
            RespuestaValidacion respuesta
                    = servicioMensajeria.validarCodigo(solicitud, tokenActivacion);
            return ResponseEntity.ok(respuesta);

        } catch (UsuarioBloqueadoExcepcion ex) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(errorBody(429, "USUARIO_BLOQUEADO", ex.getMessage()));

        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest()
                    .body(errorBody(400, "CODIGO_INCORRECTO", ex.getMessage()));

        } catch (IllegalStateException ex) {
            return ResponseEntity.status(HttpStatus.GONE)
                    .body(errorBody(410, "CODIGO_EXPIRADO_O_NO_ENCONTRADO", ex.getMessage()));
        }
    }

    // =========================================================================
    // Helper
    // =========================================================================
    private Map<String, Object> errorBody(int estado, String error, String mensaje) {
        return Map.of(
                "estado", estado,
                "error", error,
                "mensaje", mensaje,
                "fechaHora", LocalDateTime.now().toString()
        );
    }
}
