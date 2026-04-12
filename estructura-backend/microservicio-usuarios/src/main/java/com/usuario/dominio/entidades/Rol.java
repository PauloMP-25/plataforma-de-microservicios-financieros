package com.usuario.dominio.entidades;

import jakarta.persistence.*;
import lombok.*;
/**
 * Entidad de Rol. Define los niveles de acceso disponibles en el sistema.
 * @author Paulo
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
     * Valores: ROLE_FREE, ROLE_MID, ROLE_PREMIUM
     */
    @Column(nullable = false, unique = true, length = 50)
    private String nombre;

    // -------------------------------------------------------------------------
    // Enum de referencia para uso seguro en código
    // -------------------------------------------------------------------------
    public enum NombreRol {
        ROLE_FREE,
        ROLE_MID,
        ROLE_PREMIUM
    }
}
