package com.libreria.comun.seguridad;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.libreria.comun.enums.CodigoError;
import com.libreria.comun.respuesta.ResultadoApi;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Punto de entrada de seguridad para manejar errores de autenticación (HTTP 401).
 * <p>
 * Esta clase se activa cuando un usuario intenta acceder a un recurso protegido 
 * sin proporcionar credenciales válidas o cuando el token JWT ha fallado 
 * durante la fase de filtrado.
 * </p>
 * 
 * @author Paulo Moron
 * @version 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PuntoEntradaJwt implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    /**
     * Intercepta la excepción de autenticación y genera una respuesta JSON 
     * estandarizada utilizando el formato {@link ResultadoApi}.
     * 
     * @param request       Petición HTTP entrante.
     * @param response      Respuesta HTTP saliente.
     * @param authException Excepción de autenticación capturada por Spring Security.
     * @throws IOException Si ocurre un error al escribir en el flujo de salida.
     */
    @Override
    public void commence(HttpServletRequest request, 
                         HttpServletResponse response, 
                         AuthenticationException authException) throws IOException {

        log.warn("Intento de acceso no autorizado - URI: {} - Razón: {}", 
                 request.getRequestURI(), 
                 authException.getMessage());

        // Configuración de la respuesta HTTP
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        // Construcción de la respuesta usando el estándar de LUKA APP
        ResultadoApi<Void> falla = ResultadoApi.falla(
                CodigoError.ACCESO_NO_AUTORIZADO, 
                "Autenticación requerida. Por favor, proporcione un token JWT válido.", 
                request.getRequestURI()
        );

        // Escritura del JSON en el cuerpo de la respuesta
        objectMapper.writeValue(response.getOutputStream(), falla);
    }
}
