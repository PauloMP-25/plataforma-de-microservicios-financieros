package com.libreria.comun.seguridad;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Filtro de seguridad que intercepta peticiones internas.
 * <p>
 * Si la petición incluye la cabecera X-Internal-Token y coincide con el token interno
 * configurado, se autentica la petición de manera síncrona asignándole el principal
 * del sistema y la autoridad 'ROLE_SYSTEM'.
 * </p>
 */
@Slf4j
public class FiltroAutenticacionInterna extends OncePerRequestFilter {

    private final String tokenInterno;
    private static final String HEADER_INTERNAL = "X-Internal-Token";

    public FiltroAutenticacionInterna(String tokenInterno) {
        this.tokenInterno = tokenInterno;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String tokenRecibido = request.getHeader(HEADER_INTERNAL);

        if (tokenRecibido != null && !tokenRecibido.trim().isEmpty()) {
            if (tokenInterno != null && !tokenInterno.trim().isEmpty() && tokenInterno.equals(tokenRecibido.trim())) {
                
                // Construimos un DetallesUsuario especial para llamadas de sistema
                DetallesUsuario detallesSistema = DetallesUsuario.builder()
                        .usuarioId(null) // Representa al sistema
                        .email("sistema-interno@luka.local")
                        .roles(List.of("ROLE_SYSTEM"))
                        .build();

                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        detallesSistema,
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_SYSTEM"))
                );

                SecurityContextHolder.getContext().setAuthentication(authToken);
                log.debug("FiltroInterno: Petición interna autenticada exitosamente con X-Internal-Token.");
            } else {
                log.warn("FiltroInterno: Intento de llamada interna con X-Internal-Token inválido o vacío. Origen IP: {}", request.getRemoteAddr());
            }
        }

        filterChain.doFilter(request, response);
    }
}
