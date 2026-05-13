package com.usuario.dominio.especificaciones;

import com.usuario.dominio.entidades.Rol;
import com.usuario.dominio.entidades.Usuario;
import jakarta.persistence.criteria.Join;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;

/**
 * Especificaciones dinámicas para la búsqueda y filtrado de Usuarios.
 * Implementa el Specification Pattern para desacoplar la lógica de consulta del repositorio.
 * 
 * @author Paulo
 * @version 1.0.0
 */
public class UsuarioSpecs {

    /**
     * Filtra usuarios por su estado de habilitación.
     */
    public static Specification<Usuario> esHabilitado(Boolean habilitado) {
        return (root, query, cb) -> 
            habilitado == null ? null : cb.equal(root.get("habilitado"), habilitado);
    }

    /**
     * Filtra usuarios por un rol específico.
     */
    public static Specification<Usuario> tieneRol(String nombreRol) {
        return (root, query, cb) -> {
            if (nombreRol == null || nombreRol.isEmpty()) return null;
            Join<Usuario, Rol> joinRoles = root.join("roles");
            return cb.equal(joinRoles.get("nombre"), nombreRol);
        };
    }

    /**
     * Filtra usuarios creados en un rango de fechas.
     */
    public static Specification<Usuario> creadoEntre(LocalDateTime inicio, LocalDateTime fin) {
        return (root, query, cb) -> {
            if (inicio == null && fin == null) return null;
            if (inicio != null && fin != null) return cb.between(root.get("fechaCreacion"), inicio, fin);
            if (inicio != null) return cb.greaterThanOrEqualTo(root.get("fechaCreacion"), inicio);
            return cb.lessThanOrEqualTo(root.get("fechaCreacion"), fin);
        };
    }

    /**
     * Filtra por coincidencia parcial en nombre de usuario o correo.
     */
    public static Specification<Usuario> buscarPorTexto(String texto) {
        return (root, query, cb) -> {
            if (texto == null || texto.isEmpty()) return null;
            String pattern = "%" + texto.toLowerCase() + "%";
            return cb.or(
                cb.like(cb.lower(root.get("nombreUsuario")), pattern),
                cb.like(cb.lower(root.get("correo")), pattern)
            );
        };
    }
}
