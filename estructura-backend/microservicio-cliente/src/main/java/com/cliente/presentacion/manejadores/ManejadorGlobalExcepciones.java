package com.cliente.presentacion.manejadores;

import com.cliente.aplicacion.dtos.ErrorApi;
import com.cliente.aplicacion.excepciones.AccesoDenegadoException;
import com.cliente.aplicacion.excepciones.ClienteNoEncontradoException;
import com.cliente.aplicacion.excepciones.DatosPersonalesNoEncontradosException;
import com.cliente.aplicacion.excepciones.DniDuplicadoException;
import com.cliente.aplicacion.excepciones.LimiteGastoException;
import com.cliente.aplicacion.excepciones.LimiteGastoNoEncontradoException;
import com.cliente.aplicacion.excepciones.MetaNoEncontradaException;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;
import org.springframework.web.context.request.WebRequest;

/**
 * Manejador global de excepciones del microservicio-cliente. Convierte TODAS
 * las excepciones en respuestas JSON uniformes con {@link ErrorApi}.
 *
 * Cubre tanto las excepciones originales del proyecto (AccesoDenegadoException,
 * ClienteNoEncontradoException, DniDuplicadoException) como las nuevas
 * (DatosPersonalesNoEncontradosException, MetaNoEncontradaException,
 * LimiteGastoDuplicadoException).
 */
@RestControllerAdvice
@Slf4j
public class ManejadorGlobalExcepciones {

    // ── 1. Acceso denegado (403) ───────────────────────────────────────────────
    /**
     * Lanzada cuando un usuario intenta modificar el perfil de otro usuario.
     *
     * @param ex
     * @param req
     * @return
     */
    @ExceptionHandler(AccesoDenegadoException.class)
    public ResponseEntity<ErrorApi> manejarAccesoDenegado(AccesoDenegadoException ex,
            HttpServletRequest req) {
        return build(HttpStatus.FORBIDDEN, "ACCESO_DENEGADO", ex.getMessage(), req);
    }

    // ── 2. Recursos no encontrados (404) ──────────────────────────────────────
    /**
     * Lanzada por servicios antiguos (ClienteNoEncontradoException). Se
     * conserva para compatibilidad hacia atrás.
     *
     * @param ex
     * @param req
     * @return
     */
    @ExceptionHandler(ClienteNoEncontradoException.class)
    public ResponseEntity<ErrorApi> manejarClienteNoEncontrado(ClienteNoEncontradoException ex,
            HttpServletRequest req) {
        return build(HttpStatus.NOT_FOUND, "CLIENTE_NO_ENCONTRADO", ex.getMessage(), req);
    }

    /**
     * Lanzada cuando no existen DatosPersonales para un usuarioId.
     *
     * @param ex
     * @param req
     * @return
     */
    @ExceptionHandler(DatosPersonalesNoEncontradosException.class)
    public ResponseEntity<ErrorApi> manejarDatosNoEncontrados(DatosPersonalesNoEncontradosException ex,
            HttpServletRequest req) {
        return build(HttpStatus.NOT_FOUND, "DATOS_NO_ENCONTRADOS", ex.getMessage(), req);
    }

    /**
     * Lanzada cuando no existe una MetaAhorro con el UUID dado.
     *
     * @param ex
     * @param req
     * @return
     */
    @ExceptionHandler(MetaNoEncontradaException.class)
    public ResponseEntity<ErrorApi> manejarMetaNoEncontrada(MetaNoEncontradaException ex,
            HttpServletRequest req) {
        return build(HttpStatus.NOT_FOUND, "META_NO_ENCONTRADA", ex.getMessage(), req);
    }

    // ── 3. Conflictos de datos (409) ──────────────────────────────────────────
    /**
     * Lanzada cuando el DNI ya está registrado por otro usuario.
     *
     * @param ex
     * @param req
     * @return
     */
    @ExceptionHandler(DniDuplicadoException.class)
    public ResponseEntity<ErrorApi> manejarDniDuplicado(DniDuplicadoException ex,
            HttpServletRequest req) {
        return build(HttpStatus.CONFLICT, "DNI_DUPLICADO", ex.getMessage(), req);
    }

    // 4. Validación Bean Validation (400)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorApi> manejarValidaciones(MethodArgumentNotValidException ex,
            HttpServletRequest req) {
        String errores = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return build(HttpStatus.BAD_REQUEST, "DATOS_INVALIDOS", errores, req);
    }

    // 5. Seguridad JWT (401)
    @ExceptionHandler(SignatureException.class)
    public ResponseEntity<ErrorApi> manejarFirmaJwt(SignatureException ex,
            HttpServletRequest req) {
        log.error("Error firma JWT: {}", ex.getMessage());
        return build(HttpStatus.UNAUTHORIZED, "JWT_FIRMA_INVALIDA",
                "Token alterado o clave inválida.", req);
    }

    @ExceptionHandler(ExpiredJwtException.class)
    public ResponseEntity<ErrorApi> manejarTokenExpirado(ExpiredJwtException ex,
            HttpServletRequest req) {
        return build(HttpStatus.UNAUTHORIZED, "JWT_EXPIRADO", "El token ha caducado.", req);
    }

    // 6. Error genérico (500)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorApi> manejarErrorGeneral(Exception ex,
            HttpServletRequest req) {
        log.error("Error no controlado en {}: ", req.getRequestURI(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "ERROR_INTERNO",
                "Ocurrió un error inesperado en el servidor.", req);
    }

    // =========================================================================
    // Excepciones Personalizadas de LUKA APP
    // =========================================================================
    @ExceptionHandler(LimiteGastoException.class)
    public ResponseEntity<ErrorApi> manejarLimiteGlobalExistente(LimiteGastoException ex, WebRequest request) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorApi.of(409, "LIMITE_GLOBAL_EXISTENTE", ex.getMessage(), extraerRuta(request)));
    }

    @ExceptionHandler(LimiteGastoNoEncontradoException.class)
    public ResponseEntity<ErrorApi> manejarLimiteGlobalNoEncontrado(LimiteGastoNoEncontradoException ex, WebRequest request) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorApi.of(401, "LIMITE_GLOBAL_NO_ENCONTRADO", ex.getMessage(), extraerRuta(request)));
    }

    // ── Utilidad ───────────────────────────────────────────────────────────────
    private ResponseEntity<ErrorApi> build(HttpStatus status, String codigo,
            String mensaje, HttpServletRequest req) {
        return ResponseEntity
                .status(status)
                .body(ErrorApi.of(status.value(), codigo, mensaje, req.getRequestURI()));
    }

    // =========================================================================
    // Helper
    // =========================================================================
    private String extraerRuta(WebRequest request) {
        return request.getDescription(false).replace("uri=", "");
    }
}
