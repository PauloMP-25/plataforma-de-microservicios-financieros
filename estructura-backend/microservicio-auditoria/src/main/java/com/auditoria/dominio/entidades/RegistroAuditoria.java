package com.auditoria.dominio.entidades;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Entidad de persistencia que representa un registro de auditoría general en el
 * sistema.
 * <p>
 * Esta clase mapea la tabla {@code registros_auditoria} y se utiliza para
 * capturar eventos significativos realizados por los usuarios en los distintos
 * módulos de <b>Luka App</b>.
 * Incluye optimizaciones a nivel de base de datos mediante índices para mejorar
 * la velocidad de búsqueda por módulo, fecha y usuario.
 * </p>
 * 
 * @author Paulo Moron
 * @version 1.1.0
 * @since 2026-05-10
 */
@Entity
@Table(name = "registros_auditoria", indexes = {
        @Index(name = "idx_auditoria_modulo", columnList = "modulo"),
        @Index(name = "idx_auditoria_fecha", columnList = "fecha_hora"),
        @Index(name = "idx_auditoria_modulo_fecha", columnList = "modulo, fecha_hora")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegistroAuditoria {

    /**
     * Identificador único universal (UUID) del registro de auditoría.
     */
    @Id
    @GeneratedValue
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    /**
     * Identificador único (UUID) del usuario que realizó la acción.
     */
    @Column(name = "usuario_id", nullable = false)
    private UUID usuarioId;

    /**
     * Descripción breve de la acción realizada (ej. "INICIO_SESION",
     * "CREACION_USUARIO").
     */
    @Column(name = "accion", nullable = false, length = 100)
    private String accion;

    /**
     * Nombre del módulo o microservicio donde se originó el evento.
     */
    @Column(name = "modulo", nullable = false, length = 100)
    private String modulo;

    /**
     * Dirección IP desde la cual se realizó la petición. Soporta formatos IPv4 e
     * IPv6.
     */
    @Column(name = "ip_origen", length = 45)
    private String ipOrigen;

    /**
     * Información adicional detallada sobre el evento en formato de texto libre o
     * JSON.
     */
    @Column(name = "detalles", columnDefinition = "TEXT")
    private String detalles;

    /**
     * Fecha en la que se registró el evento de auditoría.
     */
    @Column(name = "fecha_hora", nullable = false)
    private LocalDate fechaHora;

    /**
     * Método de ciclo de vida de JPA ejecutado antes de persistir la entidad.
     * <p>
     * Garantiza que el campo {@code fechaHora} siempre tenga un valor si no se
     * proporcionó uno manualmente.
     * </p>
     */
    @PrePersist
    protected void alCrear() {
        if (this.fechaHora == null) {
            this.fechaHora = LocalDate.now();
        }
    }
}