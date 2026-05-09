package com.mensajeria.presentacion.controladores;

import com.mensajeria.aplicacion.dtos.*;
import com.mensajeria.aplicacion.servicios.ServicioMensajeria;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/mensajeria/otp")
@RequiredArgsConstructor
@Slf4j
public class ControladorMensajeria {

    private final ServicioMensajeria servicioMensajeria;

    /**
     * 1. GENERAR CÓDIGO Punto de entrada único para generar OTP de Registro o
     * de Reset.
     *
     * @param solicitud
     * @return
     */
    @PostMapping("/generar")
    public ResponseEntity<RespuestaGeneracion> generarCodigo(
            @Valid @RequestBody SolicitudGenerarCodigo solicitud) {

        log.debug("[POST] /otp/generar — usuarioId: {}, propósito: {}",
                solicitud.usuarioId(), solicitud.proposito());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(servicioMensajeria.generarYEnviarCodigo(solicitud));
    }

    /**
     * 2. VALIDAR ACTIVACIÓN (Flujo Registro) Activa la cuenta del usuario si el
     * código es correcto.
     *
     * @param solicitud
     * @return
     */
    @PostMapping("/validar-activacion")
    public ResponseEntity<RespuestaValidacion> validarActivacion(
            @Valid @RequestBody SolicitudValidarCodigo solicitud) {

        log.debug("[POST] /otp/validar-activacion — usuarioId: {}", solicitud.usuarioId());

        return ResponseEntity.ok(servicioMensajeria.validarParaActivacion(solicitud));
    }

    /**
     * 3. VALIDAR RECUPERACIÓN (Flujo Reset) Usado por Microservicio-Usuario
     * para validar el código de cambio de clave. Retorna el UUID del usuario
     * asociado al código.
     *
     * @param codigo
     * @return
     */
    @GetMapping("/validar-recuperacion")
    public ResponseEntity<UUID> validarRecuperacion(
            @RequestParam("usuarioId") UUID usuarioId,
            @RequestParam("codigo") String codigo) {

        log.debug("[GET] /otp/validar-recuperacion — Iniciando validación de reset");

        return ResponseEntity.ok(servicioMensajeria.validarCodigoYObtenerUsuario(usuarioId, codigo));
    }

    @PostMapping("/validar-limite")
    public ResponseEntity<Void> validarLimite(@RequestBody SolicitudGenerarCodigo solicitud) {
        // Aquí solo llamamos a verificarLimiteDiario y verificarBloqueo
        servicioMensajeria.verificarRestricciones(solicitud.usuarioId(), solicitud.proposito());
        return ResponseEntity.ok().build();
    }
}
