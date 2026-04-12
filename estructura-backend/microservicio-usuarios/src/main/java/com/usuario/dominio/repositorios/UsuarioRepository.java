package com.usuario.dominio.repositorios;

import com.usuario.dominio.entidades.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
/**
 *
 * @author user
 */
public class UsuarioRepository extends JpaRepository<Usuario, Long>{
    Optional<Usuario> findByNombreUsuario(String nombreUsuario);

    Optional<Usuario> findByCorreo(String correo);

    boolean existsByNombreUsuario(String nombreUsuario);

    boolean existsByCorreo(String correo);

    Optional<Usuario> findByNombreUsuarioAndRolesIsNotNull(String nombreUsuario);
    Optional<Usuario> findByNombreUsuarioConRoles(String nombreUsuario);
}
