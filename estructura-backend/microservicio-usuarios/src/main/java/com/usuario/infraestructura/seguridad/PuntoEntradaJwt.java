package com.usuario.infraestructura.seguridad;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.usuario.aplicacion.dtos.ErrorApi;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import java.io.IOException;

/**
 * Manejador de errores 401 No Autorizado.
 * Se ejecuta cuando se intenta acceder a un recurso protegido sin autenticación válida.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class PuntoEntradaJwt implements AuthenticationEntryPoint {
    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException excepcionAutenticacion) throws IOException {
        log.warn("Acceso no autorizado a '{}': {}",
                request.getRequestURI(),
                excepcionAutenticacion.getMessage()
        );
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        ErrorApi error = ErrorApi.of(
                HttpStatus.UNAUTHORIZED.value(),
                "NO_AUTORIZADO",
                "Autenticación requerida. Proporcione un token JWT válido.",
                request.getRequestURI()
        );

        objectMapper.writeValue(response.getOutputStream(), error);
    }
}