package com.usuario.dominio.entidades;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
/**
 * Token de confirmación de correo electrónico. Vida útil: 24 horas.
 * Se vincula muchos a uno con un usuario pendiente de activar.
 * @author Paulo
 */
@Entity
@Table(name = "tokens_confirmacion_email")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TokenConfirmacionEmail {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String token;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "expira_en", nullable = false)
    private LocalDateTime expiraEn;
    @Column(name = "confirmado_en")
    private LocalDateTime confirmadoEn;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    /**
     * Verifica si el token ha expirado.
     */
    public boolean estaExpirado() {
        return LocalDateTime.now().isAfter(expiraEn);
    }

    /**
     * Verifica si el token ya fue confirmado.
     */
    public boolean estaConfirmado() {
        return confirmadoEn != null;
    }
    
    @PrePersist
    protected void alCrear() {
        fechaCreacion = LocalDateTime.now();
    }
}
