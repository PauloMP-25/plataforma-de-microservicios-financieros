package com.usuario.dominio.repositorios;

import com.usuario.dominio.entidades.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import org.springframework.data.repository.query.Param;
/**
 *
 * @author user
 */
public interface UsuarioRepository extends JpaRepository<Usuario, Long>{
    Optional<Usuario> findByNombreUsuario(String nombreUsuario);

    Optional<Usuario> findByCorreo(String correo);

    boolean existsByNombreUsuario(String nombreUsuario);

    boolean existsByCorreo(String correo);

    Optional<Usuario> findByNombreUsuarioAndRolesIsNotNull(String nombreUsuario);

    // Usamos JPQL para traer al usuario y sus roles en una sola consulta
    @Query("SELECT u FROM Usuario u LEFT JOIN FETCH u.roles WHERE u.nombreUsuario = :nombreUsuario")
    Optional<Usuario> findByNombreUsuarioConRoles(@Param("nombreUsuario") String nombreUsuario);
}
