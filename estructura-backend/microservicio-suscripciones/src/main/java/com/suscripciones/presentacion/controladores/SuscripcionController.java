package com.suscripciones.presentacion.controladores;

import com.libreria.comun.respuesta.ResultadoApi;
import com.libreria.comun.utilidades.UtilidadSeguridad;
import com.suscripciones.aplicacion.dtos.*;
import com.suscripciones.aplicacion.puertos.ISuscripcionService;
import com.suscripciones.dominio.excepciones.ExcepcionAccesoDenegadoSuscripcion;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controlador REST para la gestión de suscripciones de usuario.
 * Proporciona endpoints para crear, buscar, listar, pagar, cancelar y editar suscripciones.
 * Todos los endpoints están securizados y validan el contexto del usuario autenticado.
 */
@RestController
@RequestMapping("/api/v1/suscripciones")
@RequiredArgsConstructor
@Validated
@Slf4j
public class SuscripcionController {

    private final ISuscripcionService suscripcionService;

    /**
     * Crea una nueva suscripción de usuario.
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ResultadoApi<RespuestaSuscripcion>> crearSuscripcion(
            @Valid @RequestBody SolicitudCrearSuscripcion solicitud) {
        
        UUID authUsuarioId = UtilidadSeguridad.obtenerUsuarioId();
        log.info("[CONTROLLER] Solicitud para crear suscripción '{}' para el usuario {}", solicitud.nombre(), solicitud.usuarioId());
        
        // Validación: Un usuario normal solo puede crear suscripciones para sí mismo.
        if (!authUsuarioId.equals(solicitud.usuarioId())) {
            log.warn("[CONTROLLER] Intento no autorizado: El usuario {} intentó crear una suscripción para el usuario {}", authUsuarioId, solicitud.usuarioId());
            throw new ExcepcionAccesoDenegadoSuscripcion(
                    "No está autorizado para crear una suscripción para otro usuario."
            );
        }

        RespuestaSuscripcion respuesta = suscripcionService.crearSuscripcion(solicitud);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ResultadoApi.creado(respuesta, "Suscripción creada exitosamente"));
    }

    /**
     * Busca una suscripción por su ID.
     */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ResultadoApi<RespuestaSuscripcion>> buscarPorId(@PathVariable UUID id) {
        log.info("[CONTROLLER] Buscando suscripción por ID: {}", id);
        RespuestaSuscripcion respuesta = suscripcionService.buscarPorId(id);
        
        // Validación de pertenencia
        UUID authUsuarioId = UtilidadSeguridad.obtenerUsuarioId();
        if (!authUsuarioId.equals(respuesta.usuarioId())) {
            log.warn("[CONTROLLER] Intento de acceso no autorizado a suscripción {} por el usuario {}", id, authUsuarioId);
            throw new ExcepcionAccesoDenegadoSuscripcion(
                    "No está autorizado para ver esta suscripción."
            );
        }

        return ResponseEntity.ok(ResultadoApi.exito(respuesta, "Suscripción recuperada exitosamente"));
    }

    /**
     * Obtiene el listado paginado y filtrado de todas las suscripciones de un usuario.
     */
    @GetMapping("/usuario/{usuarioId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ResultadoApi<List<RespuestaSuscripcion>>> listarPorUsuario(
            @PathVariable UUID usuarioId,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String metodoPago,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate fechaVencimientoAntes,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamanio) {
        
        log.info("[CONTROLLER] Listando suscripciones paginadas para el usuario: {}", usuarioId);
        
        UUID authUsuarioId = UtilidadSeguridad.obtenerUsuarioId();
        if (!authUsuarioId.equals(usuarioId)) {
            log.warn("[CONTROLLER] Intento no autorizado: El usuario {} intentó listar suscripciones del usuario {}", authUsuarioId, usuarioId);
            throw new ExcepcionAccesoDenegadoSuscripcion(
                    "No está autorizado para listar las suscripciones de otro usuario."
            );
        }

        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(
                pagina, 
                tamanio, 
                org.springframework.data.domain.Sort.by("fechaCreacion").descending()
        );

        org.springframework.data.domain.Page<RespuestaSuscripcion> page = 
                suscripcionService.listarPorUsuario(usuarioId, estado, metodoPago, fechaVencimientoAntes, pageable);

        return ResponseEntity.ok(ResultadoApi.exito(
                page.getContent(),
                "Suscripciones del usuario recuperadas exitosamente",
                com.libreria.comun.respuesta.Paginacion.desde(page)
        ));
    }

    /**
     * Registra manualmente el pago de una suscripción.
     * Requiere una cabecera 'Idempotency-Key' para prevenir reintentos accidentales.
     */
    @PostMapping("/{id}/pagar")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ResultadoApi<RespuestaPagoSuscripcion>> registrarPagoManual(
            @PathVariable UUID id,
            @Valid @RequestBody SolicitudRegistrarPagoManual solicitud,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        
        log.info("[CONTROLLER] Registrando pago manual para suscripción ID: {} con clave de idempotencia: {}", id, idempotencyKey);
        
        // Primero verificamos que la suscripción pertenezca al usuario autenticado
        RespuestaSuscripcion suscripcion = suscripcionService.buscarPorId(id);
        UUID authUsuarioId = UtilidadSeguridad.obtenerUsuarioId();
        if (!authUsuarioId.equals(suscripcion.usuarioId())) {
            log.warn("[CONTROLLER] Intento no autorizado de registrar pago para suscripción {} por el usuario {}", id, authUsuarioId);
            throw new ExcepcionAccesoDenegadoSuscripcion(
                    "No está autorizado para registrar pagos en esta suscripción."
            );
        }

        RespuestaPagoSuscripcion respuesta = suscripcionService.registrarPagoManual(id, solicitud, idempotencyKey);
        return ResponseEntity.ok(ResultadoApi.exito(respuesta, "Pago registrado exitosamente"));
    }

    /**
     * Cancela una suscripción activa.
     */
    @PostMapping("/{id}/cancelar")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ResultadoApi<RespuestaSuscripcion>> cancelarSuscripcion(@PathVariable UUID id) {
        log.info("[CONTROLLER] Solicitud de cancelación para suscripción ID: {}", id);
        
        // Primero verificamos que la suscripción pertenezca al usuario autenticado
        RespuestaSuscripcion suscripcion = suscripcionService.buscarPorId(id);
        UUID authUsuarioId = UtilidadSeguridad.obtenerUsuarioId();
        if (!authUsuarioId.equals(suscripcion.usuarioId())) {
            log.warn("[CONTROLLER] Intento no autorizado de cancelar suscripción {} por el usuario {}", id, authUsuarioId);
            throw new ExcepcionAccesoDenegadoSuscripcion(
                    "No está autorizado para cancelar esta suscripción."
            );
        }

        RespuestaSuscripcion respuesta = suscripcionService.cancelarSuscripcion(id);
        return ResponseEntity.ok(ResultadoApi.exito(respuesta, "Suscripción cancelada exitosamente"));
    }

    /**
     * Edita los campos permitidos de una suscripción existente (monto, metodoPago, tipoEstrategia).
     */
    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ResultadoApi<RespuestaSuscripcion>> editarSuscripcion(
            @PathVariable UUID id,
            @Valid @RequestBody SolicitudEditarSuscripcion solicitud) {
        
        log.info("[CONTROLLER] Editando suscripción ID: {}", id);
        
        // Primero verificamos que la suscripción pertenezca al usuario autenticado
        RespuestaSuscripcion suscripcion = suscripcionService.buscarPorId(id);
        UUID authUsuarioId = UtilidadSeguridad.obtenerUsuarioId();
        if (!authUsuarioId.equals(suscripcion.usuarioId())) {
            log.warn("[CONTROLLER] Intento no autorizado de editar suscripción {} por el usuario {}", id, authUsuarioId);
            throw new ExcepcionAccesoDenegadoSuscripcion(
                    "No está autorizado para editar esta suscripción."
            );
        }

        RespuestaSuscripcion respuesta = suscripcionService.editarSuscripcion(id, solicitud);
        return ResponseEntity.ok(ResultadoApi.exito(respuesta, "Suscripción editada exitosamente"));
    }
}
