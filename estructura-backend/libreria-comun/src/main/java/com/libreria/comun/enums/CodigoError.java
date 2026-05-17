package com.libreria.comun.enums;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Catálogo centralizado de errores para LUKA APP.
 * Define la relación entre el nombre del error y su estado HTTP.
 */
@Getter
public enum CodigoError {
    // 404 - Not Found
    RECURSO_NO_ENCONTRADO(HttpStatus.NOT_FOUND),
    CLIENTE_NO_ENCONTRADO(HttpStatus.NOT_FOUND),
    META_NO_ENCONTRADA(HttpStatus.NOT_FOUND),
    CODIGO_INVALIDO(HttpStatus.NOT_FOUND),

    // 403 - Forbidden
    ACCESO_DENEGADO(HttpStatus.FORBIDDEN),
    CUENTA_NO_ACTIVADA(HttpStatus.FORBIDDEN),
    CUENTA_BLOQUEADA(HttpStatus.FORBIDDEN),

    // 401 - Unauthorized
    ACCESO_NO_AUTORIZADO(HttpStatus.UNAUTHORIZED),
    USUARIO_NO_REGISTRADO(HttpStatus.UNAUTHORIZED),
    TOKEN_EXPIRADO(HttpStatus.UNAUTHORIZED),
    CREDENCIALES_INVALIDAS(HttpStatus.UNAUTHORIZED),

    // 409 - Conflict
    CONFLICTO_DE_DATOS(HttpStatus.CONFLICT),
    DNI_DUPLICADO(HttpStatus.CONFLICT),
    USUARIO_DUPLICADO(HttpStatus.CONFLICT),
    LIMITE_GLOBAL_EXISTENTE(HttpStatus.CONFLICT),

    // 429 - Too Many Requests
    IP_BLOQUEADA(HttpStatus.TOO_MANY_REQUESTS),
    LIMITE_DIARIO_EXCEDIDO(HttpStatus.TOO_MANY_REQUESTS),

    // 410 - Gone
    CODIGO_VENCIDO(HttpStatus.GONE),

    // 400 - Bad Request
    ERROR_VALIDACION(HttpStatus.BAD_REQUEST),
    TOKEN_INVALIDO(HttpStatus.BAD_REQUEST),
    PASSWORD_MISMATCH(HttpStatus.BAD_REQUEST),
    SOLICITUD_INCORRECTA(HttpStatus.BAD_REQUEST),
    PAGO_RECHAZADO(HttpStatus.BAD_REQUEST),
    PLAN_NO_SOPORTADO(HttpStatus.BAD_REQUEST),

    // 502 - External Error
    ERROR_SERVICIO_EXTERNO(HttpStatus.BAD_GATEWAY),

    // 500 - Internal
    ERROR_INTERNO(HttpStatus.INTERNAL_SERVER_ERROR);

    private final HttpStatus status;

    CodigoError(HttpStatus status) {
        this.status = status;
    }
}
