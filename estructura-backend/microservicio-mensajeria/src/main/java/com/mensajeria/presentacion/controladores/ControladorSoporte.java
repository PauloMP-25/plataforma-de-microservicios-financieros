package com.mensajeria.presentacion.controladores;

import com.mensajeria.aplicacion.dtos.solicitudes.SolicitudReporte;
import com.mensajeria.aplicacion.dtos.solicitudes.SolicitudSoporte;
import com.mensajeria.aplicacion.puertos.IEmailService;
import com.libreria.comun.respuesta.ResultadoApi;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * Controlador REST para el módulo de soporte al cliente.
 * <p>
 * Expone dos endpoints autenticados (requieren JWT) que procesan los formularios
 * del Centro de Asistencia ("Contactar soporte" y "Reportar un problema") y envían
 * un correo real al equipo de soporte usando el {@link IEmailService} existente.
 * </p>
 * <p>
 * <strong>Independencia de OTP:</strong> Este controlador opera sobre la ruta
 * {@code /api/v1/mensajeria/soporte} y no modifica ni reutiliza ninguna lógica de
 * los flujos de OTP, activación de cuenta o recuperación de contraseña.
 * </p>
 *
 * @version 1.0.0
 */
@RestController
@RequestMapping("/api/v1/mensajeria/soporte")
@RequiredArgsConstructor
@Slf4j
public class ControladorSoporte {

    private final IEmailService emailService;

    /**
     * Procesa el formulario "Contactar soporte" y envía un correo al equipo de soporte.
     *
     * <p>Acepta {@code multipart/form-data} para permitir el adjunto opcional de evidencia
     * (JPG, PNG, WEBP · máx. 5 MB).</p>
     *
     * @param solicitud DTO con {@code asunto}, {@code categoria} y {@code mensaje}
     *                  serializado como JSON en la parte {@code solicitud} del multipart.
     * @param adjunto   Imagen adjunta opcional.
     * @return HTTP 200 con confirmación.
     */
    @PostMapping(value = "/contacto", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResultadoApi<Void>> enviarContacto(
            @Valid @RequestPart("solicitud") SolicitudSoporte solicitud,
            @RequestPart(value = "adjunto", required = false) MultipartFile adjunto) {

        log.info("[POST] /mensajeria/soporte/contacto — categoría: '{}', asunto: '{}'",
                solicitud.categoria(), solicitud.asunto());

        emailService.enviarEmailSoporte(
                solicitud.asunto(),
                solicitud.categoria(),
                solicitud.mensaje(),
                adjunto
        );

        return ResponseEntity.ok(
                ResultadoApi.sinContenido("Tu consulta fue enviada correctamente. Te responderemos pronto."));
    }

    /**
     * Procesa el formulario "Reportar un problema" y envía un correo al equipo técnico.
     *
     * <p>Acepta {@code multipart/form-data} para permitir el adjunto opcional de evidencia
     * (JPG, PNG, WEBP · máx. 5 MB).</p>
     *
     * @param solicitud DTO con todos los campos del reporte serializado como JSON
     *                  en la parte {@code solicitud} del multipart.
     * @param adjunto   Imagen adjunta opcional como evidencia del problema.
     * @return HTTP 200 con confirmación.
     */
    @PostMapping(value = "/reporte", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResultadoApi<Void>> enviarReporte(
            @Valid @RequestPart("solicitud") SolicitudReporte solicitud,
            @RequestPart(value = "adjunto", required = false) MultipartFile adjunto) {

        log.info("[POST] /mensajeria/soporte/reporte — prioridad: '{}', sección: '{}', problema: '{}'",
                solicitud.prioridad(), solicitud.seccion(), solicitud.descripcionCorta());

        emailService.enviarEmailReporte(
                solicitud.descripcionCorta(),
                solicitud.prioridad(),
                solicitud.seccion(),
                solicitud.descripcion(),
                solicitud.queHacias(),
                adjunto
        );

        return ResponseEntity.ok(
                ResultadoApi.sinContenido("Tu reporte fue enviado correctamente. Nuestro equipo lo revisará a la brevedad."));
    }
}
