package com.cliente.dominio.entidades;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Perfil financiero del cliente.
 * Contiene información de ocupación, ingresos, estilo de vida y tono preferido
 * para la IA del microservicio-nucleo-financiero.
 * Relación 1:1 con el usuarioId del microservicio IAM.
 */
@Entity
@Table(
    name = "perfiles_financieros",
    indexes = {
        @Index(name = "idx_perfil_financiero_usuario_id", columnList = "usuario_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PerfilFinanciero {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    /** Referencia al usuario del microservicio IAM */
    @Column(name = "usuario_id", nullable = false, unique = true)
    private UUID usuarioId;

    @Column(length = 100)
    private String ocupacion;

    /**
     * Ingreso mensual en soles (PEN).
     * Precisión: 12 dígitos totales, 2 decimales.
     */
    @Column(name = "ingreso_mensual", precision = 12, scale = 2)
    private BigDecimal ingresoMensual;

    /**
     * Estilo de vida del cliente para personalización de consejos.
     * Valores sugeridos: AHORRATIVO, MODERADO, GASTADOR, INVERSOR
     */
    @Column(name = "estilo_vida", length = 30)
    private String estiloVida;

    /**
     * Tono preferido para las respuestas de la IA.
     * Valores sugeridos: FORMAL, AMIGABLE, MOTIVADOR, DIRECTO
     */
    @Column(name = "tono_ia", length = 30)
    private String tonoIA;

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
}
