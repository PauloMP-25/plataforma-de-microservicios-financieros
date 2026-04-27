package com.usuario.dominio.repositorios;

import com.usuario.dominio.entidades.Usuario;
import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

/**
 *
 * @author user
 */
public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {

    Optional<Usuario> findByCorreo(String correo);

    Optional<Usuario> findByNombreUsuario(String nombreUsuario);
    
    Optional<Usuario> findByCorreoAndHabilitadoTrue(String correo);

    boolean existsByNombreUsuario(String nombreUsuario);

    boolean existsByCorreo(String correo);

    /**
     * Elimina usuarios que no han activado su cuenta tras un periodo de tiempo.
     *
     * @param fechaLimite Punto de corte (24 horas atrás).
     * @return Cantidad de registros eliminados.
     */
    @Modifying
    @Transactional
    @Query(value = "DELETE FROM usuarios WHERE habilitado = false AND fecha_creacion < :fechaLimite", nativeQuery = true)
    int limpiarUsuariosNoActivados(@Param("fechaLimite") LocalDateTime fechaLimite);

    // Usamos JPQL para traer al usuario y sus roles en una sola consulta
    @Query("SELECT u FROM Usuario u LEFT JOIN FETCH u.roles WHERE u.nombreUsuario = :nombreUsuario")
    Optional<Usuario> findByNombreUsuarioConRoles(@Param("nombreUsuario") String nombreUsuario);
}
