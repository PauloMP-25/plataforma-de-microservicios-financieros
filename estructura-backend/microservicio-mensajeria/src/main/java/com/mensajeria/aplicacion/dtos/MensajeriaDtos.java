package com.mensajeria.aplicacion.dtos;

import com.mensajeria.dominio.entidades.CodigoVerificacion.TipoVerificacion;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

// ═══════════════════════════════════════════════════════════════
// DTOs de ENTRADA
// ═══════════════════════════════════════════════════════════════

/**
 * Solicitud de generación de código OTP.
 * Enviada por el Microservicio-Usuario o directamente por el frontend.
 */
public class MensajeriaDtos {

    /**
     * Solicitud para generar y enviar un código OTP.
     */
    public record SolicitudGenerarCodigo(

        @NotNull(message = "El usuarioId es obligatorio")
        UUID usuarioId,

        @NotBlank(message = "El email es obligatorio")
        @Email(message = "El email debe tener un formato válido")
        String email,

        /** Número de teléfono con código de país (+51...). Requerido solo si tipo=PHONE. */
        String telefono,

        @NotNull(message = "El tipo es obligatorio (EMAIL o PHONE)")
        TipoVerificacion tipo

    ) {}

    /**
     * Solicitud para validar un código OTP ingresado por el usuario.
     */
    public record SolicitudValidarCodigo(

        @NotNull(message = "El usuarioId es obligatorio")
        UUID usuarioId,

        @NotBlank(message = "El código es obligatorio")
        String codigo

    ) {}

    // ═══════════════════════════════════════════════════════════════
    // DTOs de SALIDA
    // ═══════════════════════════════════════════════════════════════

    /**
     * Respuesta estándar tras generar un código.
     */
    public record RespuestaGeneracion(
        boolean exito,
        String mensaje,
        String tipo      // "EMAIL" | "PHONE"
    ) {}

    /**
     * Respuesta estándar tras validar un código.
     */
    public record RespuestaValidacion(
        boolean exito,
        String mensaje
    ) {}

    // ═══════════════════════════════════════════════════════════════
    // DTO hacia Microservicio-Auditoría (espejo del ms-usuario)
    // ═══════════════════════════════════════════════════════════════

    public record RegistroAuditoriaDTO(
        java.time.LocalDateTime fechaHora,
        String nombreUsuario,   // Se pasa el usuarioId.toString() cuando no hay username disponible
        String accion,
        String detalles,
        String ipOrigen,
        String modulo
    ) {
        /** Constructor de conveniencia sin fechaHora. */
        public RegistroAuditoriaDTO(String nombreUsuario, String accion,
                                    String detalles, String ipOrigen, String modulo) {
            this(java.time.LocalDateTime.now(), nombreUsuario, accion, detalles, ipOrigen, modulo);
        }
    }
}
