package com.usuario.infraestructura.seguridad;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.usuario.aplicacion.dtos.ErrorApi;
import com.usuario.aplicacion.servicios.ServicioBloqueoIp;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filtro HTTP que verifica si una IP está bloqueada
 * antes de procesar la autenticación.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class FiltroRateLimitIp extends OncePerRequestFilter {

    private static final String RUTA_LOGIN = "/api/v1/auth/login";
    private final ServicioBloqueoIp servicioBloqueoIp;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        if (!esPeticionLogin(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        String ipCliente = extraerIpCliente(request);

        log.debug("FiltroRateLimitIp → {} desde IP: {}", request.getRequestURI(), ipCliente);

        if (servicioBloqueoIp.bloqueado(ipCliente)) {long minutosRestantes = servicioBloqueoIp.minutosParaDesbloqueo(ipCliente);

            log.warn("IP bloqueada: {} ({} min restantes)", ipCliente, minutosRestantes);

            escribirRespuestaBloqueo(
                    response,
                    ipCliente,
                    minutosRestantes,
                    request.getRequestURI()
            );

            return;
        }

        filterChain.doFilter(request, response);
    }
    // =========================================================================
    // Helpers
    // =========================================================================

    private boolean esPeticionLogin(HttpServletRequest request) {
        return "POST".equalsIgnoreCase(request.getMethod())
                && request.getRequestURI().contains("/auth/login");
    }

    private String extraerIpCliente(HttpServletRequest request) {

        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (tieneValor(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (tieneValor(xRealIp)) {
            return xRealIp.trim();
        }
        String cfIp = request.getHeader("CF-Connecting-IP");
        if (tieneValor(cfIp)) {
            return cfIp.trim();
        }

        return request.getRemoteAddr();
    }

    private boolean tieneValor(String header) {
        return header != null && !header.isBlank() && !"unknown".equalsIgnoreCase(header);
    }

    private void escribirRespuestaBloqueo(
            HttpServletResponse response,
            String ip,
            long minutos,
            String ruta
    ) throws IOException {response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        response.setHeader("Retry-After", String.valueOf(minutos * 60));

        ErrorApi error = ErrorApi.of(
                HttpStatus.TOO_MANY_REQUESTS.value(),
                "IP_BLOQUEADA",
                String.format(
                        "Demasiados intentos fallidos. Intente nuevamente en %d minuto(s).",
                        minutos
                ),
                ruta
        );

        objectMapper.writeValue(response.getOutputStream(), error);
    }
}
