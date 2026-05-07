package com.auditoria.dominio.entidades;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Lista negra de IPs bloqueadas por comportamiento malicioso.
 * El microservicio-gateway consulta este registro antes de procesar
 * cualquier petición entrante.
 *
 * La IP es la clave primaria: garantiza unicidad y búsqueda O(1).
 */
@Entity
@Table(
    name = "lista_negra_ip",
    indexes = {
        @Index(name = "idx_lista_negra_expiracion", columnList = "fecha_expiracion")
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ListaNegraIp {

    @Id
    @Column(name = "ip", nullable = false, length = 45)
    private String ip;

    @Column(name = "motivo", nullable = false, length = 300)
    private String motivo;

    @Column(name = "fecha_bloqueo", nullable = false)
    private LocalDateTime fechaBloqueo;

    /**
     * Fecha a partir de la cual el bloqueo expira automáticamente.
     * null = bloqueo permanente.
     */
    @Column(name = "fecha_expiracion")
    private LocalDateTime fechaExpiracion;

    // ─── Métodos de dominio ───────────────────────────────────────────────────

    /**
     * Evalúa si el bloqueo ya venció y la IP puede volver a intentar acceso.
     */
    public boolean estaActivo() {
        if (fechaExpiracion == null) {
            return true; // Bloqueo permanente
        }
        return LocalDateTime.now().isBefore(fechaExpiracion);
    }

    /**
     * Extiende el bloqueo por los minutos indicados desde ahora.
     */
    public void extenderBloqueo(long minutos) {
        this.fechaExpiracion = LocalDateTime.now().plusMinutes(minutos);
    }

    @PrePersist
    protected void alCrear() {
        if (fechaBloqueo == null) {
            fechaBloqueo = LocalDateTime.now();
        }
    }
}
