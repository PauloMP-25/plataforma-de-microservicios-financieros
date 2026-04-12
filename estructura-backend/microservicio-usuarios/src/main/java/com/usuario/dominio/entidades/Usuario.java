package com.usuario.dominio.entidades;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Paulo
 * Entidad principal de usuario. Implementa UserDetails de Spring Security
 * para integración directa con el contexto de autenticación.
 */
@Entity
@Table(name = "usuarios")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Usuario implements UserDetails{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String username;

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
    private Set<Role> roles = new HashSet<>();

    // -------------------------------------------------------------------------
    // UserDetails overrides
    // -------------------------------------------------------------------------

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles.stream()
                .map(rol -> new SimpleGrantedAuthority(rol.getName()))
                .collect(Collectors.toSet());
    }

    @Override
    public boolean isAccountNonExpired()  { return true; }

    @Override
    public boolean isAccountNonLocked()   { return accountNonLocked; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return enabled; }

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
}

