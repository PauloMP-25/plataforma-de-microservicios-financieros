package com.mensajeria.aplicacion.dtos.solicitudes;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO para el formulario "Contactar soporte" del Centro de Asistencia.
 * <p>
 * El adjunto (evidencia opcional) se recibe como {@code MultipartFile}
 * directamente en el controlador, no forma parte de este record.
 * </p>
 *
 * @param asunto    Asunto breve de la consulta.
 * @param categoria Categoría seleccionada por el usuario (Cuenta, Pagos, Límites, etc.).
 * @param mensaje   Cuerpo detallado de la consulta.
 */
public record SolicitudSoporte(

        @NotBlank(message = "El asunto no puede estar vacío.")
        @Size(min = 5, max = 100, message = "El asunto debe tener entre 5 y 100 caracteres.")
        String asunto,

        @NotBlank(message = "La categoría no puede estar vacía.")
        String categoria,

        @NotBlank(message = "El mensaje no puede estar vacío.")
        @Size(min = 20, max = 1000, message = "El mensaje debe tener entre 20 y 1000 caracteres.")
        String mensaje
) {}
