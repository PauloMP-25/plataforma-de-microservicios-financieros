package com.auditoria.dominio.entidades;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Entidad de persistencia que gestiona el bloqueo de direcciones IP maliciosas.
 * <p>
 * Funciona como el catálogo central de seguridad que es consultado de forma
 * prioritaria por el <b>microservicio-gateway</b> antes de procesar cualquier
 * petición entrante a <b>Luka App</b>.
 * </p>
 * <p>
 * Se utiliza la dirección IP como clave primaria para garantizar la unicidad
 * de los registros y permitir búsquedas de alta eficiencia (complejidad O(1)).
 * </p>
 *
 * @author Paulo Moron
 * @version 1.1.0
 * @since 2026-05-10
 */
@Entity
@Table(name = "lista_negra_ip", indexes = {
        @Index(name = "idx_lista_negra_expiracion", columnList = "fecha_expiracion")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ListaNegraIp {

    /**
     * Dirección IP bloqueada. Soporta formatos IPv4 e IPv6.
     * Actúa como identificador único del registro.
     */
    @Id
    @Column(name = "ip", nullable = false, length = 45)
    private String ip;

    /**
     * Descripción detallada del motivo que originó el bloqueo (ej. "Múltiples
     * fallos de autenticación").
     */
    @Column(name = "motivo", nullable = false, length = 300)
    private String motivo;

    /**
     * Fecha y hora en la que se registró el bloqueo inicial.
     */
    @Column(name = "fecha_bloqueo", nullable = false)
    private LocalDateTime fechaBloqueo;

    /**
     * Marca temporal que indica cuándo caduca el bloqueo automáticamente.
     * <p>
     * Un valor {@code null} indica que el bloqueo es de carácter permanente
     * hasta que sea revocado manualmente por un administrador.
     * </p>
     */
    @Column(name = "fecha_expiracion")
    private LocalDateTime fechaExpiracion;

    // ─── Métodos de dominio ───────────────────────────────────────────────────

    /**
     * Evalúa la vigencia actual del bloqueo.
     * 
     * @return {@code true} si el bloqueo es permanente o si la fecha de expiración
     *         es posterior a la hora actual; {@code false} en caso contrario.
     */
    public boolean estaActivo() {
        if (fechaExpiracion == null) {
            return true; // Bloqueo permanente
        }
        return LocalDateTime.now().isBefore(fechaExpiracion);
    }

    /**
     * Incrementa la duración del bloqueo activo.
     * 
     * @param minutos Cantidad de minutos a sumar a partir del instante actual
     *                para definir la nueva fecha de expiración.
     */
    public void extenderBloqueo(long minutos) {
        this.fechaExpiracion = LocalDateTime.now().plusMinutes(minutos);
    }

    /**
     * Método de ciclo de vida de JPA ejecutado previo a la persistencia inicial.
     * <p>
     * Asigna automáticamente la fecha de bloqueo actual si no ha sido definida.
     * </p>
     */
    @PrePersist
    protected void alCrear() {
        if (fechaBloqueo == null) {
            fechaBloqueo = LocalDateTime.now();
        }
    }
}