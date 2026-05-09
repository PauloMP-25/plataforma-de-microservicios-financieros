package com.libreria.comun.seguridad;

import com.libreria.comun.excepciones.ExcepcionNoAutorizado;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filtro de seguridad que valida el token JWT e inyecta un objeto
 * {@link DetallesUsuario} en el contexto de seguridad de Spring.
 * <p>
 * Este proceso permite que cualquier microservicio acceda al {@code usuarioId},
 * {@code email} y {@code roles} del usuario actual sin realizar consultas
 * adicionales a la base de datos.
 * </p>
 *
 * @author Paulo Moron
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FiltroJwt extends OncePerRequestFilter {

    private final ServicioJwt servicioJwt;

    private static final String CABECERA_AUTH = "Authorization";
    private static final String PREFIJO_BEARER = "Bearer ";

    /**
     * Proceso principal de filtrado. Extrae el token e inyecta la autenticación
     * si es válido.
     *
     * @param request Objeto con la información de la petición HTTP.
     * @param response Objeto para gestionar la respuesta HTTP.
     * @param filterChain Cadena de filtros de seguridad.
     * @throws ServletException Si ocurre un error en el servlet.
     * @throws IOException Si ocurre un error de entrada/salida.
     */
    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        final String cabeceraAuth = request.getHeader(CABECERA_AUTH);

        // Validar presencia de cabecera Bearer
        if (cabeceraAuth == null || !cabeceraAuth.startsWith(PREFIJO_BEARER)) {
            filterChain.doFilter(request, response);
            return;
        }

        final String jwt = cabeceraAuth.substring(PREFIJO_BEARER.length());

        try {
            String email = servicioJwt.extraerSubject(jwt);

            // Si el token tiene un subject y no hay autenticación previa en el hilo actual
            if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                if (servicioJwt.esTokenValido(jwt)) {
                    // 1. Construimos el objeto DetallesUsuario con la info del Token
                    DetallesUsuario detalles = DetallesUsuario.builder()
                            .usuarioId(servicioJwt.extraerUsuarioId(jwt))
                            .email(email)
                            .roles(servicioJwt.extraerRoles(jwt))
                            .build();

                    // 2. Creamos el token de autenticación usando el objeto 'detalles' como Principal
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            detalles,
                            null,
                            detalles.getAuthorities()
                    );

                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    // 3. Inyectamos en el contexto
                    SecurityContextHolder.getContext().setAuthentication(authToken);

                    log.debug("Autenticación Stateless exitosa para usuarioId: '{}'", detalles.getUsuarioId());
                }
            }
        } catch (ExcepcionNoAutorizado e) {
            log.warn("Fallo en validación de token: {}", e.getMensaje());
            SecurityContextHolder.clearContext();
        } catch (Exception e) {
            log.error("Error inesperado en el filtro de seguridad: {}", e.getMessage());
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Determina si el filtro debe omitirse para ciertas rutas (Whitelist).
     *
     * @param request Petición actual.
     * @return true si la ruta es pública y no debe filtrarse.
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return path.startsWith("/api/v1/auth/")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/swagger-ui/")
                || path.startsWith("/actuator/");
    }
}
