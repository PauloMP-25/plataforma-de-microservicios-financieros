package com.mensajeria.aplicacion.dtos.solicitudes;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO para el formulario "Reportar un problema" del Centro de Asistencia.
 * <p>
 * El adjunto (evidencia opcional) se recibe como {@code MultipartFile}
 * directamente en el controlador, no forma parte de este record.
 * </p>
 *
 * @param descripcionCorta Resumen breve del problema.
 * @param prioridad        Prioridad indicada por el usuario: baja, media o alta.
 * @param seccion          Sección de la aplicación donde ocurrió el problema.
 * @param descripcion      Descripción detallada del problema.
 * @param queHacias        Contexto opcional: qué estaba haciendo el usuario cuando ocurrió.
 */
public record SolicitudReporte(

        @NotBlank(message = "La descripción corta no puede estar vacía.")
        @Size(min = 5, max = 200, message = "La descripción corta debe tener entre 5 y 200 caracteres.")
        String descripcionCorta,

        @NotBlank(message = "La prioridad no puede estar vacía.")
        String prioridad,

        @NotBlank(message = "La sección no puede estar vacía.")
        String seccion,

        @NotBlank(message = "La descripción no puede estar vacía.")
        @Size(min = 20, max = 2000, message = "La descripción debe tener entre 20 y 2000 caracteres.")
        String descripcion,

        @Size(max = 500, message = "El campo '¿Qué estabas haciendo?' no puede superar los 500 caracteres.")
        String queHacias
) {}
