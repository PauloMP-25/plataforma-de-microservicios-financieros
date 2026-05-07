package com.auditoria.dominio.entidades;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Registra cada intento de acceso al sistema (login exitoso o fallido).
 * Es la fuente de verdad para detectar ataques de fuerza bruta por IP.
 */
@Entity
@Table(
    name = "auditoria_accesos",
    indexes = {
        @Index(name = "idx_acceso_ip_fecha",   columnList = "ip_origen, fecha"),
        @Index(name = "idx_acceso_usuario_id", columnList = "usuario_id"),
        @Index(name = "idx_acceso_estado",     columnList = "estado")
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditoriaAcceso {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    /**
     * UUID del usuario que intentó autenticarse.
     * Puede ser null si el usuario no existe en el sistema.
     */
    @Column(name = "usuario_id")
    private UUID usuarioId;

    @Column(name = "ip_origen", nullable = false, length = 45)
    private String ipOrigen;

    @Column(name = "navegador", length = 500)
    private String navegador;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private EstadoAcceso estado;

    @Column(name = "detalle_error", length = 500)
    private String detalleError;

    @Column(name = "fecha", nullable = false)
    private LocalDateTime fecha;

    public enum EstadoAcceso {
        EXITO,
        FALLO
    }

    @PrePersist
    protected void alCrear() {
        if (fecha == null) {
            fecha = LocalDateTime.now();
        }
    }
}
