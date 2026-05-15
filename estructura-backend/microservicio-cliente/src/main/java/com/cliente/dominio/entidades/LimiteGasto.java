package com.cliente.dominio.entidades;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Límite de gasto mensual por categoría.
 *
 * La categoría se almacena como String libre para que sea compatible con los
 * nombres generales que devuelve el microservicio de clasificación Python (ej:
 * "Galletas", "Transporte", "Estudios", "Salud", etc.).
 *
 * El microservicio-nucleo-financiero consulta estos límites para alertar al
 * usuario cuando se acerca o supera el umbral configurado.
 */
@Entity
@Table(
        name = "limites_gasto",
        indexes = {
            @Index(name = "idx_limite_gasto_usuario_id", columnList = "usuario_id"),
        },
        uniqueConstraints = {
            // Un usuario solo puede tener un límite marcado como activo físicamente
            @UniqueConstraint(name = "uq_limite_global_usuario", columnNames = {"usuario_id", "activo"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LimiteGasto {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
        @Column(updatable = false, nullable = false)
        private UUID id;

    /**
     * Referencia al usuario propietario del límite
     */
    @Column(name = "usuario_id", nullable = false)
    private UUID usuarioId;

    /**
     * Monto máximo permitido para esta categoría en el mes (en soles PEN).
     */
    @Column(name = "monto_limite", nullable = false, precision = 12, scale = 2)
    private BigDecimal montoLimite;

    /**
     * Porcentaje de alerta (0-100). Cuando el gasto alcance este % del límite
     * se publicará el evento LIMITE_ALCANZADO. Por defecto: 80 (avisa al llegar
     * al 80% del límite).
     */
    @Column(name = "porcentaje_alerta", nullable = false)
    @Builder.Default
    private Integer porcentajeAlerta = 80;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin", nullable = false)
    private LocalDate fechaFin;

    @Column(nullable = false)
    private boolean activo; // Para eliminación lógica o desactivación manual

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion", nullable = false)
    private LocalDateTime fechaActualizacion;

    @PrePersist
    protected void alCrear() {
        fechaCreacion = LocalDateTime.now();
        fechaActualizacion = LocalDateTime.now();
        this.activo = true;
    }

    @PreUpdate
    protected void alActualizar() {
        fechaActualizacion = LocalDateTime.now();
    }

    public boolean estaVencido() {
        return LocalDate.now().isAfter(fechaFin);
    }

    /**
     * Evalúa si el gasto actual ha alcanzado o superado el umbral de alerta.
     *
     * @param gastoTotalPeriodo monto gastado este mes
     * @return true si se debe publicar el evento LIMITE_ALCANZADO
     */
    public boolean haAlcanzadoUmbral(BigDecimal gastoTotalPeriodo) {
        if (montoLimite == null || montoLimite.compareTo(BigDecimal.ZERO) == 0) {
            return false;
        }
        BigDecimal umbral = montoLimite
                .multiply(BigDecimal.valueOf(porcentajeAlerta))
                .divide(BigDecimal.valueOf(100));
        return gastoTotalPeriodo.compareTo(umbral) >= 0;
    }
}
