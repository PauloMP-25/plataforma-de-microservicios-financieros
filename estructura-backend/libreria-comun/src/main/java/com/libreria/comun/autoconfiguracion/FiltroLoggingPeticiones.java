package com.libreria.comun.autoconfiguracion;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filtro HTTP que registra todas las peticiones entrantes de forma limpia
 * para evitar verbosidad, y silencia explícitamente los endpoints de actuator
 * (como /actuator/health) para no ensuciar los logs de los contenedores.
 */
@Slf4j
public class FiltroLoggingPeticiones extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        long startTime = System.currentTimeMillis();
        try {
            filterChain.doFilter(request, response);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            log.info("HTTP {} {} - {} {}ms", request.getMethod(), request.getRequestURI(), response.getStatus(), duration);
        }
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        String path = request.getRequestURI();
        // Silenciar los logs de health checks
        return path != null && path.contains("/actuator/health");
    }
}
