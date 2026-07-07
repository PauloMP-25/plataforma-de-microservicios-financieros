package com.cliente.dominio.entidades;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Meta de ahorro del cliente.
 * Registra un objetivo financiero con su progreso actual.
 */
@Entity
@Table(
    name = "metas_ahorro",
    indexes = {
        @Index(name = "idx_meta_ahorro_usuario_id",  columnList = "usuario_id"),
        @Index(name = "idx_meta_ahorro_completada",  columnList = "completada")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MetaAhorro {

    @Id
    @GeneratedValue
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    /** Referencia al usuario propietario de la meta */
    @Column(name = "usuario_id", nullable = false)
    private UUID usuarioId;

    @Column(nullable = false, length = 150)
    private String nombre;

    /**
     * Propósito / Categoría de la meta de ahorro (ej. Viaje, Auto, etc.)
     */
    @Column(name = "proposito", length = 100)
    private String proposito;

    /**
     * Monto total a alcanzar (en soles PEN).
     */
    @Column(name = "monto_objetivo", nullable = false, precision = 12, scale = 2)
    private BigDecimal montoObjetivo;

    /**
     * Monto ahorrado hasta el momento.
     */
    @Column(name = "monto_actual", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal montoActual = BigDecimal.ZERO;

    @Column(name = "fecha_inicio")
    private LocalDate fechaInicio;

    /**
     * Fecha límite para cumplir la meta. Puede ser nula (sin límite).
     */
    @Column(name = "fecha_limite")
    private LocalDate fechaObjetivo;

    @Column(name = "fecha_completada")
    private LocalDate fechaCompletada;

    @Column(nullable = false)
    @Builder.Default
    private Boolean completada = false;

    @Column(nullable = false, columnDefinition = "boolean default true")
    @Builder.Default
    private Boolean activa = true;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion", nullable = false)
    private LocalDateTime fechaActualizacion;

    @PrePersist
    protected void alCrear() {
        fechaCreacion      = LocalDateTime.now();
        fechaActualizacion = LocalDateTime.now();
    }

    @PreUpdate
    protected void alActualizar() {
        fechaActualizacion = LocalDateTime.now();
    }

    // =========================================================================
    // Lógica de Dominio
    // =========================================================================

    /**
     * Calcula el porcentaje de progreso hacia la meta.
     *
     * @return valor entre 0.00 y 100.00 redondeado a 2 decimales.
     */
    public BigDecimal calcularPorcentajeProgreso() {
        if (montoObjetivo == null || montoObjetivo.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal progreso = montoActual
                .divide(montoObjetivo, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);

        // No superar el 100%
        return progreso.min(BigDecimal.valueOf(100));
    }

    /**
     * Evalúa y actualiza el estado {@code completada} según el monto actual.
     * Devuelve {@code true} si la meta acaba de alcanzarse en esta evaluación.
     * @return 
     */
    public boolean evaluarYMarcarCompletada() {
        if (!this.completada && montoActual.compareTo(montoObjetivo) >= 0) {
            this.completada = true;
            this.fechaCompletada = LocalDate.now();
            return true; // Recién alcanzada — publicar evento
        }
        return false;
    }
}
