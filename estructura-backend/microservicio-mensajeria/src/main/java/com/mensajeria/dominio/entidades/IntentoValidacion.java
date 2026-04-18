package com.mensajeria.dominio.entidades;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Registra los intentos de validación fallidos por usuarioId. Tras
 * {@code maxIntentos} fallos consecutivos, el usuarioId queda bloqueado para
 * nuevas solicitudes durante {@code bloqueoHoras} horas.
 *
 * Persiste en BD para sobrevivir reinicios del servicio.
 */
@Entity
@Table(
        name = "intentos_validacion_otp",
        indexes = {
            @Index(name = "idx_intento_usuario_id", columnList = "usuario_id", unique = true)
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IntentoValidacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", nullable = false, unique = true, updatable = false)
    private UUID usuarioId;

    @Column(nullable = false)
    @Builder.Default
    private int intentos = 0;

    @Column(name = "ultima_modificacion", nullable = false)
    private LocalDateTime ultimaModificacion;

    @Column(name = "bloqueado", nullable = false)
    @Builder.Default
    private boolean bloqueado = false;

    @Column(name = "bloqueado_hasta")
    private LocalDateTime bloqueadoHasta;

    // ─── Métodos de dominio ───────────────────────────────────────────────────
    public void incrementarIntentos() {
        this.intentos++;
        this.ultimaModificacion = LocalDateTime.now();
    }

    public void bloquear(long horas) {
        this.bloqueado = true;
        this.bloqueadoHasta = LocalDateTime.now().plusHours(horas);
        this.ultimaModificacion = LocalDateTime.now();
    }

    public void reiniciar() {
        this.intentos = 0;
        this.bloqueado = false;
        this.bloqueadoHasta = null;
        this.ultimaModificacion = LocalDateTime.now();
    }

    public boolean bloqueoExpirado() {
        return bloqueadoHasta != null && LocalDateTime.now().isAfter(bloqueadoHasta);
    }

    @PrePersist
    protected void alCrear() {
        if (ultimaModificacion == null) {
            ultimaModificacion = LocalDateTime.now();
        }
    }
}
