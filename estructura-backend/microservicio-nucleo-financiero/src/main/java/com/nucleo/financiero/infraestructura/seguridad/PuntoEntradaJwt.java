package com.nucleo.financiero.infraestructura.seguridad;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nucleo.financiero.aplicacion.dtos.auditoria.ErrorApi;
import jakarta.servlet.http.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import java.io.IOException;

@Component
@Slf4j
@RequiredArgsConstructor
public class PuntoEntradaJwt implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException ex) throws IOException {
        log.warn("Acceso no autorizado a '{}': {}", request.getRequestURI(), ex.getMessage());
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getOutputStream(),
                ErrorApi.of(401, "NO_AUTORIZADO",
                        "Autenticación requerida. Proporcione un token JWT válido.",
                        request.getRequestURI()));
    }
}
