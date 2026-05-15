package com.auditoria.dominio.entidades;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Entidad de persistencia encargada de la trazabilidad detallada de cambios en
 * el negocio.
 * <p>
 * Proporciona un registro histórico de las modificaciones realizadas sobre
 * entidades críticas.
 * Almacena instantáneas del estado anterior y posterior en formato JSON,
 * permitiendo la
 * reconstrucción de estados previos y cumpliendo con estándares internacionales
 * de auditoría y cumplimiento (como PCIDSS y SOX) requeridos en el sector
 * financiero.
 * </p>
 * 
 * @author Paulo Moron
 * @version 1.1.0
 * @since 2026-05-10
 */
@Entity
@Table(name = "auditoria_transaccional", indexes = {
        @Index(name = "idx_transac_usuario_id", columnList = "usuario_id"),
        @Index(name = "idx_transac_entidad_id", columnList = "entidad_id"),
        @Index(name = "idx_transac_servicio_origen", columnList = "servicio_origen"),
        @Index(name = "idx_transac_fecha", columnList = "fecha")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditoriaTransaccional {

    /**
     * Identificador único universal (UUID) del registro transaccional.
     */
    @Id
    @GeneratedValue
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    /**
     * Identificador único (UUID) del usuario responsable de la modificación.
     */
    @Column(name = "usuario_id", nullable = false)
    private UUID usuarioId;

    /**
     * Identificador único (UUID) de la entidad de negocio que fue afectada en su
     * microservicio de origen.
     */
    @Column(name = "entidad_id", nullable = false)
    private UUID entidadId;

    /**
     * Nombre del microservicio que generó el evento (ej:
     * "MICROSERVICIO-FINANCIERO").
     */
    @Column(name = "servicio_origen", nullable = false, length = 100)
    private String servicioOrigen;

    /**
     * Nombre técnico de la entidad de negocio modificada (ej: "Transaccion",
     * "Cliente").
     */
    @Column(name = "entidad_afectada", nullable = false, length = 100)
    private String entidadAfectada;

    /**
     * Descripción textual de la acción ejecutada sobre la entidad.
     */
    @Column(name = "descripcion", nullable = false, length = 50)
    private String descripcion;

    /**
     * Representación serializada (JSON) del estado de la entidad antes del cambio.
     * Es vital para procesos de reversión o análisis de discrepancias.
     */
    @Column(name = "valor_anterior", columnDefinition = "TEXT")
    private String valorAnterior;

    /**
     * Representación serializada (JSON) del nuevo estado de la entidad tras la
     * operación.
     * Permite auditar exactamente qué campos fueron alterados.
     */
    @Column(name = "valor_nuevo", columnDefinition = "TEXT")
    private String valorNuevo;

    /**
     * Fecha de registro de la transacción de auditoría.
     */
    @Column(name = "fecha", nullable = false)
    private LocalDate fecha;

    /**
     * Método de ciclo de vida de JPA ejecutado antes de la persistencia.
     * <p>
     * Garantiza que el campo {@code fecha} esté poblado automáticamente al momento
     * de la creación.
     * </p>
     */
    @PrePersist
    protected void alCrear() {
        if (fecha == null) {
            fecha = LocalDate.now();
        }
    }
}