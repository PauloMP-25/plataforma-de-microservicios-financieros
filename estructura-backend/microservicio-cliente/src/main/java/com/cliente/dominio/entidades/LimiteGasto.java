package com.cliente.dominio.entidades;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Límite de gasto mensual por categoría.
 *
 * La categoría se almacena como String libre para que sea compatible con los
 * nombres generales que devuelve el microservicio de clasificación Python
 * (ej: "Galletas", "Transporte", "Estudios", "Salud", etc.).
 *
 * El microservicio-nucleo-financiero consulta estos límites para alertar al
 * usuario cuando se acerca o supera el umbral configurado.
 */
@Entity
@Table(
    name = "limites_gasto",
    indexes = {
        @Index(name = "idx_limite_gasto_usuario_id",  columnList = "usuario_id"),
        @Index(name = "idx_limite_gasto_categoria",   columnList = "categoria_id")
    },
    uniqueConstraints = {
        // Un usuario solo puede tener un límite por categoría
        @UniqueConstraint(
            name  = "uq_limite_usuario_categoria",
            columnNames = { "usuario_id", "categoria_id" }
        )
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

    /** Referencia al usuario propietario del límite */
    @Column(name = "usuario_id", nullable = false)
    private UUID usuarioId;

    /**
     * Nombre de la categoría en texto libre.
     * Compatible con la nomenclatura del microservicio Python de categorización.
     * Ejemplos: "Galletas", "Transporte", "Estudios", "Delivery", "Salud"
     */
    @Column(name = "categoria_id", nullable = false, length = 100)
    private String categoriaId;

    /**
     * Monto máximo permitido para esta categoría en el mes (en soles PEN).
     */
    @Column(name = "monto_limite", nullable = false, precision = 12, scale = 2)
    private BigDecimal montoLimite;

    /**
     * Porcentaje de alerta (0-100). Cuando el gasto alcance este % del límite
     * se publicará el evento LIMITE_ALCANZADO.
     * Por defecto: 80 (avisa al llegar al 80% del límite).
     */
    @Column(name = "porcentaje_alerta", nullable = false)
    @Builder.Default
    private Integer porcentajeAlerta = 80;

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

    /**
     * Evalúa si el gasto actual ha alcanzado o superado el umbral de alerta.
     *
     * @param gastoActual monto gastado en la categoría este mes
     * @return true si se debe publicar el evento LIMITE_ALCANZADO
     */
    public boolean haAlcanzadoUmbral(BigDecimal gastoActual) {
        if (montoLimite == null || montoLimite.compareTo(BigDecimal.ZERO) == 0) {
            return false;
        }
        BigDecimal umbral = montoLimite
                .multiply(BigDecimal.valueOf(porcentajeAlerta))
                .divide(BigDecimal.valueOf(100));
        return gastoActual.compareTo(umbral) >= 0;
    }
}
