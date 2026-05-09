package com.libreria.comun.seguridad;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementación estandarizada de {@link UserDetails} para LUKA APP.
 * <p>
 * Este objeto actúa como el principal de seguridad en toda la plataforma. 
 * Contiene la identidad básica (email) y metadatos críticos como el {@code usuarioId} 
 * extraído del token JWT, permitiendo realizar validaciones de propiedad de recursos 
 * en cualquier microservicio.
 * </p>
 * 
 * @author Paulo Moron
 * @version 1.0.0
 */
@Getter
@Builder
@AllArgsConstructor
public class DetallesUsuario implements UserDetails {

    /** Identificador único del usuario en formato UUID. */
    private final UUID usuarioId;
    
    /** Correo electrónico del usuario (utilizado como username). */
    private final String email;
    
    /** Lista de permisos o roles asignados (ej. ROLE_USER, ROLE_ADMIN). */
    private final List<String> roles;

    /**
     * Convierte la lista de strings de roles en autoridades reconocidas por Spring Security.
     * 
     * @return Colección de autoridades otorgadas.
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
    }

    /**
     * El password no se almacena en este objeto por seguridad y por la naturaleza Stateless del JWT.
     * 
     * @return null siempre.
     */
    @Override
    public String getPassword() {
        return null;
    }

    /**
     * Devuelve el correo electrónico del usuario.
     * 
     * @return email.
     */
    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
