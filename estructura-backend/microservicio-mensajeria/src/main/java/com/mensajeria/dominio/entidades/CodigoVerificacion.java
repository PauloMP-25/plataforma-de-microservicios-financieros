package com.mensajeria.dominio.entidades;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidad principal del microservicio. Almacena los códigos OTP de 6 dígitos
 * asociados a un usuario específico. Vigencia por defecto: 10 minutos
 * (configurable).
 *
 * @author Paulo — ampliado desde CodigoVerificacion de Ikaza
 */
@Entity
@Table(
        name = "codigos_verificacion",
        indexes = {
            @Index(name = "idx_codigo_otp", columnList = "codigo"),
            @Index(name = "idx_codigo_usuario_id", columnList = "usuario_id"),
            @Index(name = "idx_codigo_fecha_expira", columnList = "fecha_expiracion"),
            @Index(name = "idx_codigo_email", columnList = "email")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CodigoVerificacion {

    @Id
    @GeneratedValue
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    /**
     * Identificador del usuario en el Microservicio-Usuario. Campo OBLIGATORIO:
     * todo código debe estar ligado a un usuario del sistema.
     */
    @Column(name = "usuario_id", nullable = false, updatable = false)
    private UUID usuarioId;

    @Column(nullable = false, length = 150)
    private String email;

    @Column(name = "telefono", length = 20)
    private String telefono;

    @Column(nullable = false, length = 6)
    private String codigo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TipoVerificacion tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PropositoCodigo proposito;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_expiracion", nullable = false)
    private LocalDateTime fechaExpiracion;

    @Column(nullable = false)
    @Builder.Default
    private Boolean usado = false;

    @Column(name = "fecha_uso")
    private LocalDateTime fechaUso;

    // ─── Enum ────────────────────────────────────────────────────────────────
    public enum TipoVerificacion {
        EMAIL, SMS
    }

    public enum PropositoCodigo {
        ACTIVACION_CUENTA, RESTABLECER_PASSWORD
    }

    // ─── Lifecycle ───────────────────────────────────────────────────────────
    @PrePersist
    protected void alCrear() {
        fechaCreacion = LocalDateTime.now();
        // La expiración puede ser sobreescrita antes del persist si viene del servicio;
        // si sigue siendo null, asignamos el valor por defecto de 10 minutos.
        if (fechaExpiracion == null) {
            fechaExpiracion = LocalDateTime.now().plusMinutes(10);
        }
    }

    // ─── Métodos de dominio ───────────────────────────────────────────────────
    /**
     * Evalúa si el código ya pasó su ventana de validez.
     *
     * @return
     */
    public boolean isExpirado() {
        return LocalDateTime.now().isAfter(fechaExpiracion);
    }

    /**
     * Valida el código ingresado contra el almacenado. Retorna true únicamente
     * si no está usado, no expiró y los valores coinciden.
     *
     * @param codigoIngresado
     * @return
     */
    public boolean isValido(String codigoIngresado) {
        return !usado && !isExpirado() && codigo.equals(codigoIngresado);
    }

    /**
     * Validación optimizada: verifica si el código coincide, no ha sido usado,
     * no ha expirado y el propósito es el correcto.
     * @param codigoIngresado
     * @param propositoRequerido
     * @return 
     */
    public boolean esValidoPara(String codigoIngresado, PropositoCodigo propositoRequerido) {
        return !usado
                && !isExpirado()
                && this.codigo.equals(codigoIngresado)
                && this.proposito == propositoRequerido;
    }
}
