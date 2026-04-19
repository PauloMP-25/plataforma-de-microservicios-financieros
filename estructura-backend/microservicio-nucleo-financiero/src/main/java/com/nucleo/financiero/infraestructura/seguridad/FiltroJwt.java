package com.nucleo.financiero.infraestructura.seguridad;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class FiltroJwt extends OncePerRequestFilter {

    private final ServicioJwt servicioJwt;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        final String cabecera = request.getHeader("Authorization");
        if (cabecera == null || !cabecera.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        final String jwt = cabecera.substring(7);
        try {
            String nombreUsuario = servicioJwt.extraerNombreUsuario(jwt);
            if (nombreUsuario != null
                    && SecurityContextHolder.getContext().getAuthentication() == null
                    && !servicioJwt.estaExpirado(jwt)) {

                List<String> roles = servicioJwt.extraerRoles(jwt);
                UsuarioPrincipal principal = new UsuarioPrincipal(nombreUsuario, roles);

                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
                log.debug("JWT válido → usuario: '{}', roles: {}", nombreUsuario, roles);
            }
        } catch (ExpiredJwtException e)  { log.warn("JWT expirado: {}", e.getMessage()); }
          catch (SignatureException e)   { log.warn("Firma inválida: {}", e.getMessage()); }
          catch (MalformedJwtException e){ log.warn("JWT malformado: {}", e.getMessage()); }
          catch (JwtException e)         { log.warn("Error JWT: {}", e.getMessage()); }

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String ruta = request.getRequestURI();
        return ruta.startsWith("/actuator/health")
                || ruta.startsWith("/v3/api-docs")
                || ruta.startsWith("/swagger-ui");
    }
}
