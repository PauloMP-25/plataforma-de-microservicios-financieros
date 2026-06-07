package com.libreria.comun.seguridad;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Interceptor Feign que propaga automáticamente la autenticación en las llamadas internas.
 * <p>
 * Si existe un contexto de petición web con un token JWT (cabecera Authorization), este
 * se propaga tal cual. En caso de no existir una sesión de usuario (ej. llamadas programadas,
 * tareas asíncronas o eventos RabbitMQ), propaga el token secreto interno en la cabecera
 * X-Internal-Token para llamadas de confianza System-to-System.
 * </p>
 */
@Slf4j
public class InterceptorFeignSeguridad implements RequestInterceptor {

    @Value("${LUKA_INTERNAL_TOKEN:}")
    private String tokenInterno;

    private static final String HEADER_AUTH = "Authorization";
    private static final String HEADER_INTERNAL = "X-Internal-Token";
    private static final String PREFIJO_BEARER = "Bearer ";

    @Override
    public void apply(RequestTemplate template) {
        // 1. Intentar propagar el JWT actual del usuario
        String jwt = obtenerJwtActual();
        if (jwt != null) {
            template.header(HEADER_AUTH, jwt);
            log.debug("Feign: Propagando JWT de usuario en la cabecera Authorization.");
            return;
        }

        // 2. Si no hay JWT de usuario, propagar el token interno para llamadas de sistema
        if (tokenInterno != null && !tokenInterno.trim().isEmpty()) {
            template.header(HEADER_INTERNAL, tokenInterno.trim());
            log.debug("Feign: Sin contexto de usuario. Propagando token interno X-Internal-Token.");
        } else {
            log.warn("Feign: No se pudo propagar JWT de usuario ni se configuró un token interno en 'luka.seguridad.token-interno'.");
        }
    }

    /**
     * Intenta extraer el JWT Bearer del contexto de petición HTTP actual.
     */
    private String obtenerJwtActual() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            String authHeader = request.getHeader(HEADER_AUTH);
            if (authHeader != null && authHeader.startsWith(PREFIJO_BEARER)) {
                return authHeader;
            }
        }
        return null;
    }
}
