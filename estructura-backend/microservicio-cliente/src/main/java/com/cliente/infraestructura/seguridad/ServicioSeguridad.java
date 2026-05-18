package com.cliente.infraestructura.seguridad;

import com.libreria.comun.seguridad.DetallesUsuario;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import java.util.UUID;

/**
 * Servicio de seguridad personalizado para validación de propiedad en expresiones SpEL.
 * Permite defensa en profundidad verificando que el usuario autenticado acceda únicamente a sus recursos.
 */
@Service("seguridadService")
public class ServicioSeguridad {

    /**
     * Verifica si el usuario autenticado coincide con el ID del recurso consultado.
     * Permitido si el usuario tiene rol ADMIN (los administradores pueden consultar cualquier perfil).
     * 
     * @param usuarioId ID del usuario propietario del recurso.
     * @param authentication Información de autenticación de Spring Security.
     * @return true si coincide el ID o si es administrador, false en caso contrario.
     */
    public boolean esElMismoUsuario(UUID usuarioId, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        // Si es administrador, tiene acceso completo
        boolean esAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        if (esAdmin) {
            return true;
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof DetallesUsuario) {
            DetallesUsuario detalles = (DetallesUsuario) principal;
            return detalles.getUsuarioId().equals(usuarioId);
        }

        return false;
    }

    /**
     * Sobrecarga sin parámetros para uso directo en expresiones SpEL sin modificar las firmas de los métodos.
     * 
     * @return true si es una llamada interna de confianza, false en caso contrario.
     */
    public boolean esServicioInterno() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes) {
            HttpServletRequest request = ((ServletRequestAttributes) attributes).getRequest();
            return esServicioInterno(request);
        }
        return false;
    }

    /**
     * Verifica si la petición proviene de un servicio interno de confianza.
     * Esto incluye:
     * 1. Peticiones de loopback/localhost (desarrollo local inter-servicio).
     * 2. Peticiones de la red interna de Docker (172.x.x.x).
     * 3. Peticiones que tengan la cabecera 'X-Gateway-Source' del gateway.
     * 4. Peticiones autenticadas como ADMIN.
     * 
     * @param request La petición HTTP actual.
     * @return true si es una llamada interna de confianza, false en caso contrario.
     */
    public boolean esServicioInterno(HttpServletRequest request) {
        if (request == null) {
            return false;
        }

        // Si ya está autenticado como administrador, es confiable
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            boolean esAdmin = auth.getAuthorities().stream()
                    .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
            if (esAdmin) {
                return true;
            }
        }

        // 1. Validar origen de cabecera del gateway
        String gatewayHeader = request.getHeader("X-Gateway-Source");
        if ("api-gateway".equalsIgnoreCase(gatewayHeader)) {
            return true;
        }

        // 2. Validar IP remota (dirección de red de confianza: localhost o subredes de docker/k8s)
        String ipRemota = request.getRemoteAddr();
        if (ipRemota == null) {
            return false;
        }

        if ("127.0.0.1".equals(ipRemota) || "0:0:0:0:0:0:0:1".equals(ipRemota) || ipRemota.startsWith("localhost")) {
            return true;
        }

        // Rangos de IPs privadas estándar (Docker networks / Kubernetes pods)
        if (ipRemota.startsWith("172.") || ipRemota.startsWith("10.") || ipRemota.startsWith("192.168.")) {
            return true;
        }

        return false;
    }
}
