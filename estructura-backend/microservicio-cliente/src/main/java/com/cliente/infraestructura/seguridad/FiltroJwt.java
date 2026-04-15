package com.cliente.infraestructura.seguridad;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.cliente.aplicacion.dtos.ErrorApi;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Valida el JWT de cada petición y carga el usuarioId en el SecurityContext
 * como un atributo de request para que el controlador lo use.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class FiltroJwt extends OncePerRequestFilter {

    private static final String CABECERA_AUTH  = "Authorization";
    private static final String PREFIJO_BEARER = "Bearer ";
    public  static final String ATTR_USUARIO_ID = "usuarioIdAutenticado";

    private final ServicioJwt  servicioJwt;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        final String cabecera = request.getHeader(CABECERA_AUTH);

        if (cabecera == null || !cabecera.startsWith(PREFIJO_BEARER)) {
            filterChain.doFilter(request, response);
            return;
        }

        final String jwt = cabecera.substring(PREFIJO_BEARER.length());

        try {
            if (!servicioJwt.esTokenValido(jwt)) {
                escribirError(response, request.getRequestURI(),
                        "Token JWT inválido o expirado.");
                return;
            }

            // Extraer datos del token
            String        nombreUsuario = servicioJwt.extraerNombreUsuario(jwt);
            UUID          usuarioId     = servicioJwt.extraerUsuarioId(jwt);
            List<String>  roles         = servicioJwt.extraerRoles(jwt);

            // Guardar usuarioId en atributo de request para uso en controladores
            request.setAttribute(ATTR_USUARIO_ID, usuarioId);

            // Inyectar autenticación en el SecurityContext
            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                var authorities = roles.stream()
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList());

                var authToken = new UsernamePasswordAuthenticationToken(
                        nombreUsuario, null, authorities);
                authToken.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }

            log.debug("JWT válido — usuario: {}, usuarioId: {}", nombreUsuario, usuarioId);

        } catch (ExpiredJwtException e) {
            log.warn("JWT expirado: {}", e.getMessage());
            escribirError(response, request.getRequestURI(), "El token JWT ha expirado.");
            return;
        } catch (JwtException e) {
            log.warn("JWT inválido: {}", e.getMessage());
            escribirError(response, request.getRequestURI(), "Token JWT inválido.");
            return;
        }

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String ruta = request.getRequestURI();
        // SUNAT e inicial son manejados por la configuración de seguridad
        return false;
    }

    private void escribirError(HttpServletResponse response,
                                String ruta, String mensaje) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getOutputStream(),
                ErrorApi.of(401, "NO_AUTORIZADO", mensaje, ruta));
    }
}
