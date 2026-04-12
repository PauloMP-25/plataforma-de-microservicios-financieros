package com.usuario.infraestructura.seguridad;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filtro que valida el JWT en el header Authorization: Bearer <token>
 * e inyecta la autenticación en el SecurityContext.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class FiltroJwt extends OncePerRequestFilter{
    private static final String CABECERA_AUTH = "Authorization";
    private static final String PREFIJO_BEARER = "Bearer ";

    private final ServicioJwt servicioJwt;
    private final UserDetailsService servicioDetallesUsuario;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        final String cabeceraAuth = request.getHeader(CABECERA_AUTH);

        if (cabeceraAuth == null || !cabeceraAuth.startsWith(PREFIJO_BEARER)) {
            filterChain.doFilter(request, response);
            return;
        }

        final String jwt = cabeceraAuth.substring(PREFIJO_BEARER.length());

        try {
            procesarJwt(jwt, request);
        } catch (ExpiredJwtException e) {
            log.warn("JWT expirado para URI '{}': {}", request.getRequestURI(), e.getMessage());
        } catch (SignatureException e) {
            log.warn("Firma JWT inválida: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.warn("JWT malformado: {}", e.getMessage());
        } catch (UsernameNotFoundException e) {
            log.warn("Usuario del JWT no encontrado: {}", e.getMessage());
        } catch (JwtException e) {
            log.warn("Error procesando JWT: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }
    // =========================================================================
    // Procesamiento
    // =========================================================================

    private void procesarJwt(String jwt, HttpServletRequest request) {

        final String nombreUsuario = servicioJwt.extraerNombreUsuario(jwt);

        if (nombreUsuario == null) return;

        boolean yaAutenticado =
                SecurityContextHolder.getContext().getAuthentication() != null;

        if (yaAutenticado) {log.trace("Ya autenticado '{}', omitiendo JWT", nombreUsuario);
            return;
        }

        UserDetails usuario = servicioDetallesUsuario.loadUserByUsername(nombreUsuario);

        if (servicioJwt.esTokenValido(jwt, usuario)) {
            inyectarAutenticacion(usuario, request);
            log.debug("JWT válido → usuario: '{}', roles: {}",
                    nombreUsuario, usuario.getAuthorities());
        }
    }
    private void inyectarAutenticacion(UserDetails usuario,
                                       HttpServletRequest request) {

        UsernamePasswordAuthenticationToken token =
                new UsernamePasswordAuthenticationToken(
                        usuario,
                        null,
                        usuario.getAuthorities()
                );

        token.setDetails(
                new WebAuthenticationDetailsSource().buildDetails(request)
        );

        SecurityContextHolder.getContext().setAuthentication(token);
        }

    // =========================================================================
    // Exclusión de endpoints públicos
    // =========================================================================

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String ruta = request.getRequestURI();

        return ruta.startsWith("/api/v1/auth/")
                || ruta.startsWith("/actuator/health")
                || ruta.startsWith("/v3/api-docs")
                || ruta.startsWith("/swagger-ui");
    }
}
