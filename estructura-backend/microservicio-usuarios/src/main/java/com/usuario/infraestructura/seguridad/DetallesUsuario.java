package com.usuario.infraestructura.seguridad;

import com.usuario.dominio.repositorios.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementación de UserDetailsService para Spring Security.
 * Carga el usuario desde PostgreSQL incluyendo sus roles (JOIN FETCH
 * para evitar LazyInitializationException fuera de sesión JPA).
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DetallesUsuario implements UserDetailsService{
    private final UsuarioRepository usuarioRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String nombreUsuario) throws UsernameNotFoundException {
        log.debug("Cargando usuario desde BD: {}", nombreUsuario);
        return usuarioRepository.findByNombreUsuarioConRoles(nombreUsuario)
            .orElseThrow(() -> {
                log.warn("Usuario no encontrado: {}", nombreUsuario);
                return new UsernameNotFoundException("Usuario no encontrado: " + nombreUsuario);
            });
    }
}
