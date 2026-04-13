package com.usuario.dominio.entidades;

import jakarta.persistence.*;
import lombok.*;
/**
 * Entidad de Rol. Define los niveles de acceso disponibles en el sistema.
 * @author Paulo
 */

@Entity
@Table(name = "roles")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Rol {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Nombre del rol. Convención Spring Security: prefijo ROLE_.
     * Valores: ROLE_FREE, ROLE_MID, ROLE_PREMIUM
     */
    @Column(nullable = false, unique = true, length = 50)
    private String nombre;

    // -------------------------------------------------------------------------
    // Enum de referencia para uso seguro en código
    // -------------------------------------------------------------------------
    public enum NombreRol {
        ROLE_ADMIN,
        ROLE_FREE,
        ROLE_MID,
        ROLE_PREMIUM
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }
    
    
}
