package com.auditoria.dominio.entidades;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Trazabilidad completa de cambios en entidades de negocio.
 * Almacena el estado anterior y posterior de cualquier modificación
 * en formato JSON para auditorías de cumplimiento (PCIDSS, SOX).
 */
@Entity
@Table(
    name = "auditoria_transaccional",
    indexes = {
        @Index(name = "idx_transac_usuario_id",       columnList = "usuario_id"),
        @Index(name = "idx_transac_entidad_id",       columnList = "entidad_id"),
        @Index(name = "idx_transac_servicio_origen",  columnList = "servicio_origen"),
        @Index(name = "idx_transac_fecha",            columnList = "fecha")
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditoriaTransaccional {

    @Id
    @GeneratedValue
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    /** UUID del usuario que realizó la acción. */
    @Column(name = "usuario_id", nullable = false)
    private UUID usuarioId;

    /** Nombre del microservicio que originó el cambio (ej: MICROSERVICIO-FINANCIERO). */
    @Column(name = "servicio_origen", nullable = false, length = 100)
    private String servicioOrigen;

    /** Nombre de la entidad de negocio modificada (ej: Transaccion, Cliente). */
    @Column(name = "entidad_afectada", nullable = false, length = 100)
    private String entidadAfectada;

    /** UUID de la entidad modificada en su microservicio de origen. */
    @Column(name = "entidad_id", nullable = false)
    private String entidadId;

    /**
     * Estado anterior de la entidad serializado en JSON.
     * Permite reconstruir el estado previo ante incidencias.
     */
    @Column(name = "valor_anterior", columnDefinition = "TEXT")
    private String valorAnterior;

    /**
     * Estado nuevo de la entidad serializado en JSON.
     * Permite auditar exactamente qué cambió.
     */
    @Column(name = "valor_nuevo", columnDefinition = "TEXT")
    private String valorNuevo;

    @Column(name = "fecha", nullable = false)
    private LocalDateTime fecha;

    @PrePersist
    protected void alCrear() {
        if (fecha == null) {
            fecha = LocalDateTime.now();
        }
    }
}
