package com.usuario.dominio.entidades;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.userdetails.UserDetails;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Entidad de dominio que representa a un Usuario en el sistema.
 * <p>
 * Implementa {@link UserDetails} para permitir que Spring Security gestione la
 * autenticación y autorización directamente con este objeto.
 * </p>
 *
 * * @author Paulo
 * @version 1.0
 */
@Entity
@Table(name = "usuarios")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Usuario implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 100)
    private String nombreUsuario;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, unique = true, length = 150)
    private String correo;

    @Column(nullable = false)
    @Builder.Default
    private boolean habilitado = false;

    @Column(name = "cuenta_no_bloqueada", nullable = false)
    @Builder.Default
    private boolean cuentaNoBloqueada = true;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion", nullable = false)
    private LocalDateTime fechaActualizacion;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "usuario_roles",
            joinColumns = @JoinColumn(name = "usuario_id"),
            inverseJoinColumns = @JoinColumn(name = "rol_id")
    )
    @Builder.Default
    private Set<Rol> roles = new HashSet<>();

    @Override
    public boolean isAccountNonLocked() {
        return this.cuentaNoBloqueada;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return this.habilitado;
    }

    // -------------------------------------------------------------------------
    // Lifecycle hooks
    // -------------------------------------------------------------------------
    @PrePersist
    protected void alCrear() {
        fechaCreacion = LocalDateTime.now();
        fechaActualizacion = LocalDateTime.now();
    }

    @PreUpdate
    protected void alActualizar() {
        fechaActualizacion = LocalDateTime.now();
    }

    /**
     * Convierte los roles de la entidad en autoridades reconocidas por Spring
     * Security.
     * @return Colección de autoridades (permisos/roles).
     */
    @Override
    public java.util.Collection<? extends org.springframework.security.core.GrantedAuthority> getAuthorities() {
        // Mapeamos tus roles a la estructura que entiende Spring Security
        return roles.stream()
                .map(rol -> new org.springframework.security.core.authority.SimpleGrantedAuthority(rol.getNombre()))
                .collect(java.util.stream.Collectors.toList());
    }

    @Override
    public String getUsername() {
        return this.correo;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }
}
