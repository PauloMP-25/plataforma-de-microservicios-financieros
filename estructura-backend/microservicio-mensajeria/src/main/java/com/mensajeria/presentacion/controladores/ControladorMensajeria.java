package com.mensajeria.presentacion.controladores;

import com.mensajeria.aplicacion.dtos.*;
import com.mensajeria.aplicacion.servicios.IMensajeriaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.libreria.comun.respuesta.ResultadoApi;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Controlador REST para la gestión de OTPs y validaciones de mensajería.
 * <p>
 * Expone los endpoints públicos que los microservicios cliente (ms-usuario) y
 * la app frontend consumen para los flujos de activación de cuenta y
 * recuperación de contraseña. La inyección se hace exclusivamente a través de
 * la interfaz {@link IMensajeriaService}, respetando el principio de inversión
 * de dependencias (SOLID-D).
 * </p>
 *
 * @author Paulo Moron
 * @version 1.1.0
 */
@RestController
@RequestMapping("/api/v1/mensajeria/otp")
@RequiredArgsConstructor
@Slf4j
public class ControladorMensajeria {

    /** Servicio inyectado por interfaz — Spring resuelve {@code MensajeriaServiceImpl}. */
    private final IMensajeriaService mensajeriaService;

    /**
     * Genera y envía un código OTP al canal elegido por el usuario.
     * <p>
     * Punto de entrada único para los flujos de activación de cuenta y
     * recuperación de contraseña. Aplica bloqueo, límite diario y throttling
     * antes de persistir el código.
     * </p>
     *
     * @param solicitud DTO con {@code usuarioId}, {@code email}, {@code telefono},
     *                  {@code tipo} (EMAIL | SMS) y {@code proposito}
     *                  (ACTIVACION_CUENTA | RESTABLECER_PASSWORD).
     * @return HTTP 201 con {@link ResultadoApi} envolviendo {@link RespuestaGeneracion}.
     */
    @PostMapping("/generar")
    public ResponseEntity<ResultadoApi<RespuestaGeneracion>> generarCodigo(
            @Valid @RequestBody SolicitudGenerarCodigo solicitud) {

        log.debug("[POST] /otp/generar — usuarioId: {}, propósito: {}",
                solicitud.usuarioId(), solicitud.proposito());

        RespuestaGeneracion respuesta = mensajeriaService.generarYEnviarCodigo(solicitud);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ResultadoApi.creado(respuesta, "Código generado exitosamente"));
    }

    /**
     * Valida el OTP en el flujo de activación de cuenta.
     * <p>
     * Si el código es correcto, notifica al ms-usuario para activar la cuenta
     * y sincroniza el teléfono verificado.
     * </p>
     *
     * @param solicitud DTO con {@code usuarioId} y el {@code codigo} OTP ingresado.
     * @return HTTP 200 con {@link ResultadoApi} envolviendo {@link RespuestaValidacion}.
     */
    @PostMapping("/validar-activacion")
    public ResponseEntity<ResultadoApi<RespuestaValidacion>> validarActivacion(
            @Valid @RequestBody SolicitudValidarCodigo solicitud) {

        log.debug("[POST] /otp/validar-activacion — usuarioId: {}", solicitud.usuarioId());

        RespuestaValidacion respuesta = mensajeriaService.validarParaActivacion(solicitud);
        return ResponseEntity.ok(ResultadoApi.exito(respuesta, "Código validado exitosamente", null));
    }

    /**
     * Valida el OTP para el flujo de recuperación de contraseña.
     * <p>
     * Consumido internamente por el ms-usuario para obtener el UUID del
     * usuario propietario del código antes de emitir el token de reset.
     * </p>
     *
     * @param usuarioId UUID del registro OTP usado como identificador del proceso.
     * @param codigo    Código OTP de 6 dígitos ingresado por el usuario.
     * @return HTTP 200 con el UUID del usuario validado, listo para el reset.
     */
    @GetMapping("/validar-recuperacion")
    public ResponseEntity<ResultadoApi<UUID>> validarRecuperacion(
            @RequestParam("usuarioId") UUID usuarioId,
            @RequestParam("codigo") String codigo) {

        log.debug("[GET] /otp/validar-recuperacion — iniciando validación de reset");

        UUID usuarioValidado = mensajeriaService.validarCodigoYObtenerUsuario(usuarioId, codigo);
        return ResponseEntity.ok(ResultadoApi.exito(usuarioValidado, "Usuario validado exitosamente", null));
    }

    /**
     * Verifica anticipadamente las restricciones de bloqueo y límite diario
     * para el usuario dado, sin generar ni enviar ningún código OTP.
     * <p>
     * Útil para que el frontend deshabilite el botón de reenvío antes de que
     * el usuario lo intente y reciba un error 429.
     * </p>
     *
     * @param solicitud DTO con {@code usuarioId} y {@code proposito} a verificar.
     * @return HTTP 200 si el usuario puede solicitar un código; error semántico
     *         del {@code ManejadorGlobalExcepciones} si está bloqueado o agotó
     *         su límite diario.
     */
    @PostMapping("/validar-limite")
    public ResponseEntity<ResultadoApi<Void>> validarLimite(
            @RequestBody SolicitudGenerarCodigo solicitud) {

        mensajeriaService.verificarRestricciones(solicitud.usuarioId(), solicitud.proposito());
        return ResponseEntity.ok(ResultadoApi.sinContenido("Restricciones verificadas exitosamente"));
    }
}

