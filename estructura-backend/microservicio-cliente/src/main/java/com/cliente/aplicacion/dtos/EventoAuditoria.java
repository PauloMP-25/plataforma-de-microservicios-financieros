package com.cliente.aplicacion.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * DTO de ENTRADA que el ms-cliente publica en la cola RabbitMQ
 * {@code cola.auditoria} para registrar eventos de negocio.
 *
 * <p>
 * El contrato de campos es idéntico al {@code RegistroAuditoriaRequestDTO} del
 * microservicio-auditoria para que Jackson deserialice sin errores en el
 * consumidor.</p>
 *
 * <p>
 * NO usa {@code EstadoAcceso} porque ese campo es exclusivo de eventos de
 * autenticación (login/logout). Los eventos de negocio del ms-cliente van a la
 * tabla {@code registros_auditoria}, NO a {@code auditoria_accesos}.</p>
 *
 * Acciones que publica este microservicio:
 * <ul>
 * <li>PERFIL_CREADO</li>
 * <li>PERFIL_ACTUALIZADO</li>
 * <li>PERFIL_FINANCIERO_ACTUALIZADO</li>
 * <li>META_CREADA</li>
 * <li>META_COMPLETADA</li>
 * <li>META_ELIMINADA</li>
 * <li>LIMITE_CREADO</li>
 * <li>LIMITE_ALCANZADO</li>
 * <li>LIMITE_ELIMINADO</li>
 * </ul>
 */
public record EventoAuditoria(
        LocalDateTime fechaHora, // null aquí — @PrePersist lo asigna en ms-auditoria

        @NotBlank
        @Size(max = 150)
        String nombreUsuario, // nombre completo o "sistema" si es automático

        @NotBlank
        @Size(max = 100)
        String accion, // ej: "META_CREADA", "PERFIL_ACTUALIZADO"

        @NotBlank
        @Size(max = 100)
        String modulo, // siempre "MICROSERVICIO-CLIENTE"

        @Size(max = 45)
        String ipOrigen, // IP real del cliente o "interno"

        String detalles // texto libre con contexto del evento
        ) implements Serializable {

    /**
     * Factory method principal. Deja {@code fechaHora} en null para que el
     * ms-auditoria la asigne en {@code @PrePersist}, garantizando consistencia
     * de zona horaria.
     */
    public static EventoAuditoria crear(
            String nombreUsuario,
            String accion,
            String modulo,
            String ipOrigen,
            String detalles
    ) {
        return new EventoAuditoria(
                null, // fechaHora — asignado por @PrePersist en ms-auditoria
                nombreUsuario,
                accion,
                modulo,
                ipOrigen,
                detalles
        );
    }

    /**
     * Sobrecarga de conveniencia que usa el módulo por defecto del ms-cliente.
     */
    public static EventoAuditoria de(
            String nombreUsuario,
            String accion,
            String ipOrigen,
            String detalles
    ) {
        return crear(nombreUsuario, accion, "MICROSERVICIO-CLIENTE", ipOrigen, detalles);
    }
}
