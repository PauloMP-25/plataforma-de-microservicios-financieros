package com.usuario.presentacion.manejadores;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.usuario.aplicacion.dtos.ErrorApi;
import com.usuario.aplicacion.dtos.EstadoAcceso;
import com.usuario.aplicacion.excepciones.IpBloqueadaException;
import com.usuario.infraestructura.mensajeria.PublicadorAuditoria;
import feign.FeignException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

/**
 * Manejador global de excepciones.
 */
@RestControllerAdvice
@Slf4j
@RequiredArgsConstructor
public class ManejadorGlobalExcepciones {

    private final PublicadorAuditoria publicador;
    private final jakarta.servlet.http.HttpServletRequest requestHttp;

    // =========================================================================
    // Excepciones de dominio
    // =========================================================================
    @ExceptionHandler(IpBloqueadaException.class)
    public ResponseEntity<ErrorApi> manejarIpBloqueada(
            IpBloqueadaException ex, WebRequest request) {

        log.warn("IP bloqueada: {} — minutos restantes: {}",
                ex.getDireccionIp(), ex.getMinutosParaDesbloqueo());

        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(ErrorApi.of(
                        429,
                        "IP_BLOQUEADA",
                        ex.getMessage(),
                        extraerRuta(request)
                ));
    }

    // =========================================================================
    // Errores de comunicacion entre microservicios
    // =========================================================================
    @ExceptionHandler(FeignException.class)
    public ResponseEntity<ErrorApi> manejarFeignException(FeignException ex, HttpServletRequest request) {
        String mensajeFinal = "Error en comunicación con servicio externo.";

        // 1. Intentamos extraer el mensaje real que mandó Mensajería
        if (ex.contentUTF8() != null && !ex.contentUTF8().isBlank()) {
            try {
                // Usamos Jackson para leer el JSON que viene en el body
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                com.fasterxml.jackson.databind.JsonNode root = mapper.readTree(ex.contentUTF8());

                if (root.has("mensaje")) {
                    mensajeFinal = root.get("mensaje").asText();
                }
            } catch (JsonProcessingException e) {
                log.warn("No se pudo parsear el error de Feign, usando mensaje genérico.");
            }
        }

        log.error("Error en comunicación externa: status={}, body={}", ex.status(), ex.contentUTF8());

        return ResponseEntity.status(ex.status() > 0 ? ex.status() : 500)
                .body(ErrorApi.of(
                        ex.status(),
                        "ERROR_MICROSERVICIO_EXTERNO",
                        mensajeFinal, // <--- Aquí ya va el mensaje real
                        request.getRequestURI()
                ));
    }

    // =========================================================================
    // Validaciones (@Valid)
    // =========================================================================
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorApi> manejarErroresValidacion(
            MethodArgumentNotValidException ex, WebRequest request) {

        List<String> detalles = ex.getBindingResult().getAllErrors().stream()
                .map(error -> {
                    if (error instanceof FieldError fe) {
                        return String.format("'%s': %s",
                                fe.getField(), fe.getDefaultMessage());
                    }
                    return error.getDefaultMessage();
                })
                .collect(Collectors.toList());

        log.debug("Errores de validación: {}", detalles);

        return ResponseEntity.badRequest()
                .body(ErrorApi.of(
                        400,
                        "ERROR_VALIDACION",
                        "Error en los datos enviados.",
                        extraerRuta(request),
                        detalles
                ));
    }

    // =========================================================================
    // Autorización
    // =========================================================================
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorApi> manejarAccesoDenegado(
            AccessDeniedException ex, WebRequest request) {

        log.warn("Acceso denegado: {}", ex.getMessage());

        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ErrorApi.of(
                        403,
                        "PROHIBIDO",
                        "No tiene permisos para acceder a este recurso.",
                        extraerRuta(request)
                ));
    }

    // =========================================================================
    // Errores de negocio
    // =========================================================================
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorApi> manejarArgumentoInvalido(
            IllegalArgumentException ex, WebRequest request) {

        return ResponseEntity.badRequest()
                .body(ErrorApi.of(
                        400,
                        "SOLICITUD_INCORRECTA",
                        ex.getMessage(),
                        extraerRuta(request)
                ));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorApi> manejarEstadoInvalido(
            IllegalStateException ex, WebRequest request) {

        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorApi.of(
                        409,
                        "CONFLICTO",
                        ex.getMessage(),
                        extraerRuta(request)
                ));
    }

    // =========================================================================
    // Catch-all
    // =========================================================================
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorApi> manejarErrorGeneral(
            Exception ex, WebRequest request) {

        log.error("Error inesperado: {}", ex.getMessage(), ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorApi.of(
                        500,
                        "ERROR_INTERNO",
                        "Ha ocurrido un error interno. Intente nuevamente más tarde.",
                        extraerRuta(request)
                ));
    }

    // =========================================================================
    // Seguridad (Cuentas bloqueadas/deshabilitadas)
    // =========================================================================
    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ErrorApi> manejarUsuarioNoEncontrado(UsernameNotFoundException ex, WebRequest request) {
        // Esto ocurre cuando el correo no existe en la BD
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ErrorApi.of(401, "USUARIO_NO_REGISTRADO",
                        "El correo ingresado no pertenece a ninguna cuenta.", extraerRuta(request)));
    }

    @ExceptionHandler(org.springframework.security.authentication.DisabledException.class)
    public ResponseEntity<ErrorApi> manejarCuentaDeshabilitada(DisabledException ex, WebRequest request) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ErrorApi.of(403, "CUENTA_DESHABILITADA", "Su cuenta aún no ha sido activada. Revise su correo electrónico.", extraerRuta(request)));
    }

    @ExceptionHandler(org.springframework.security.authentication.LockedException.class)
    public ResponseEntity<ErrorApi> manejarCuentaBloqueada(LockedException ex, WebRequest request) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ErrorApi.of(403, "CUENTA_BLOQUEADA", "Su cuenta ha sido bloqueada temporalmente.", extraerRuta(request)));
    }

    @ExceptionHandler(org.springframework.security.authentication.BadCredentialsException.class)
    public ResponseEntity<ErrorApi> manejarCredencialesInvalidas(BadCredentialsException ex, WebRequest request) {
        // 1. Recuperamos el atributo como Object
        Object idAtributo = requestHttp.getAttribute("intento_usuario_id");

        // 2. Casteo seguro: Si es UUID lo guardamos, si no, queda null
        UUID usuarioId = (idAtributo instanceof UUID) ? (UUID) idAtributo : null;

        // 3. Obtenemos la IP
        String ipCliente = requestHttp.getRemoteAddr();

        // 4. Enviamos al publicador (Asegúrate de que tu publicador acepte UUID ahora)
        publicador.publicarAcceso(usuarioId, ipCliente, EstadoAcceso.FALLO, "Intento fallido: Credenciales incorrectas", PublicadorAuditoria.RK_ACCESO_FALLO);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ErrorApi.of(401, "CREDENCIALES_INVALIDAS", "La contraseña ingresada es incorrecta.", extraerRuta(request)));
    }

// =========================================================================
// Errores de lectura de JSON
// =========================================================================
    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorApi> manejarJsonMalformado(HttpMessageNotReadableException ex, WebRequest request) {
        return ResponseEntity.badRequest()
                .body(ErrorApi.of(400, "JSON_INVALIDO", "El cuerpo de la solicitud no tiene un formato válido.", extraerRuta(request)));
    }

    // =========================================================================
    // Helper
    // =========================================================================
    private String extraerRuta(WebRequest request) {
        return request.getDescription(false).replace("uri=", "");
    }
}
