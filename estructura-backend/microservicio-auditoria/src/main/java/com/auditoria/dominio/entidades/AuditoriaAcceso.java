package com.auditoria.dominio.entidades;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

import com.libreria.comun.enums.EstadoEvento;

/**
 * Entidad de persistencia que registra cada intento de acceso al sistema.
 * <p>
 * Esta clase es la fuente de verdad para el módulo de seguridad, permitiendo
 * rastrear inicios de sesión exitosos y fallidos. Su diseño optimizado mediante
 * índices permite realizar análisis de seguridad en tiempo real para detectar
 * y mitigar ataques de fuerza bruta basados en IP o cuenta de usuario.
 * </p>
 * 
 * @author Paulo Moron
 * @version 1.1.0
 * @since 2026-05-10
 */
@Entity
@Table(name = "auditoria_accesos", indexes = {
        @Index(name = "idx_acceso_ip_fecha", columnList = "ip_origen, fecha"),
        @Index(name = "idx_acceso_usuario_id", columnList = "usuario_id"),
        @Index(name = "idx_acceso_estado", columnList = "estado")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditoriaAcceso {

    /**
     * Identificador único universal (UUID) del registro de acceso.
     */
    @Id
    @GeneratedValue
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    /**
     * Identificador único (UUID) del usuario que intentó autenticarse.
     * <p>
     * Puede ser {@code null} en casos donde el intento de acceso se realice con
     * credenciales de un usuario inexistente en el sistema.
     * </p>
     */
    @Column(name = "usuario_id")
    private UUID usuarioId;

    /**
     * Dirección IP desde la cual se originó el intento de acceso.
     */
    @Column(name = "ip_origen", nullable = false, length = 45)
    private String ipOrigen;

    /**
     * Información del User-Agent del navegador o cliente utilizado para el acceso.
     */
    @Column(name = "navegador", length = 500)
    private String navegador;

    /**
     * Estado del evento de acceso (exitoso o fallido).
     * Proviene de la enumeración estandarizada en la {@code libreria-comun}.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private EstadoEvento estado;

    /**
     * Descripción detallada del error en caso de un acceso fallido (ej. "Contraseña
     * incorrecta").
     */
    @Column(name = "detalle_error", length = 500)
    private String detalleError;

    /**
     * Fecha y hora exacta en la que se produjo el intento de acceso.
     */
    @Column(name = "fecha", nullable = false)
    private LocalDateTime fecha;

    /**
     * Método de ciclo de vida de JPA ejecutado antes de la persistencia.
     * <p>
     * Asegura la integridad temporal del registro asignando la fecha actual
     * si no se ha especificado previamente.
     * </p>
     */
    @PrePersist
    protected void alCrear() {
        if (fecha == null) {
            fecha = LocalDateTime.now();
        }
    }
}