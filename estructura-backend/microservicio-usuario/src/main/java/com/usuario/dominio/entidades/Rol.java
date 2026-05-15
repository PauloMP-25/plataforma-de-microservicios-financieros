package com.usuario.dominio.entidades;

import jakarta.persistence.*;
import lombok.*;

/**
 * Entidad de Rol. Define los niveles de acceso disponibles en el sistema.
 * <p>
 * Los roles soportados son: FREE, PRO, PREMIUM, ADMIN y ADMINISTRADOR.
 * </p>
 * 
 * @author Paulo
 * @version 1.1.0
 */
@Entity
@Table(name = "roles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Rol {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Nombre del rol. Convención Spring Security: prefijo ROLE_.
     */
    @Column(nullable = false, unique = true, length = 50)
    private String nombre;

    // -------------------------------------------------------------------------
    // Enum de referencia para uso seguro en código
    // -------------------------------------------------------------------------
    public enum NombreRol {
        ROLE_ADMIN,
        ROLE_ADMINISTRADOR,
        ROLE_FREE,
        ROLE_PRO,
        ROLE_PREMIUM
    }

    /**
     * Helper para obtener el enum a partir del nombre del rol.
     * @return El enum correspondiente.
     */
    public NombreRol getNombreAsEnum() {
        return NombreRol.valueOf(this.nombre);
    }
}
