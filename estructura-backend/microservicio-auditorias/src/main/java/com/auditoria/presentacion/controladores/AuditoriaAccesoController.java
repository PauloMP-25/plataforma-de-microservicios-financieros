package com.auditoria.presentacion.controladores;

import com.auditoria.aplicacion.dtos.*;
import com.auditoria.aplicacion.servicios.ServicioSeguridadAuditoria;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * Controlador para la auditoría de accesos y trazabilidad transaccional.
 *
 * Rutas:
 *  POST  /api/v1/auditoria/accesos              → Registrar intento de acceso
 *  GET   /api/v1/auditoria/accesos              → Listar accesos paginados
 *  GET   /api/v1/auditoria/verificar-ip/{ip}    → Verificar si IP está bloqueada
 *  POST  /api/v1/auditoria/transacciones        → Registrar cambio de entidad
 *  GET   /api/v1/auditoria/transacciones        → Listar cambios con filtros
 */
@RestController
@RequestMapping("/api/v1/auditoria")
@RequiredArgsConstructor
@Slf4j
public class AuditoriaAccesoController {

    private final ServicioSeguridadAuditoria servicioSeguridad;

    // ─── Accesos ──────────────────────────────────────────────────────────────

    /**
     * Recibe reportes del API Gateway o del Microservicio-Usuario cuando ocurre
     * un intento de login. Si el estado es FALLO, desencadena la lógica de
     * detección de fuerza bruta automáticamente.
     */
    @PostMapping("/accesos")
    public ResponseEntity<AuditoriaAccesoDTO> registrarAcceso(
            @Valid @RequestBody AuditoriaAccesoRequestDTO request) {

        log.debug("POST /accesos — ip={}, estado={}", request.ipOrigen(), request.estado());
        AuditoriaAccesoDTO creado = servicioSeguridad.registrarAcceso(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    /**
     * Historial de accesos paginado. Protegido para ROLE_ADMIN.
     */
    @GetMapping("/accesos")
    public ResponseEntity<Page<AuditoriaAccesoDTO>> listarAccesos(
            @RequestParam(defaultValue = "0")  int pagina,
            @RequestParam(defaultValue = "20") int tamanio) {

        Pageable paginacion = PageRequest.of(
                Math.max(0, pagina),
                Math.min(tamanio, 100),
                Sort.by("fecha").descending()
        );
        return ResponseEntity.ok(servicioSeguridad.listarAccesos(paginacion));
    }

    // ─── Verificación de IP ───────────────────────────────────────────────────

    /**
     * Endpoint clave para el API Gateway: devuelve si la IP está en lista negra.
     * Diseñado para ser rápido (índice en la PK de ListaNegraIp).
     *
     * Respuesta 200 con { bloqueada: true/false } — nunca 404.
     */
    @GetMapping("/verificar-ip/{ip}")
    public ResponseEntity<RespuestaVerificacionIpDTO> verificarIp(@PathVariable String ip) {
        log.debug("GET /verificar-ip/{}", ip);
        return ResponseEntity.ok(servicioSeguridad.verificarIp(ip));
    }

    // ─── Trazabilidad Transaccional ────────────────────────────────────────────

    /**
     * Registra un cambio en una entidad de negocio.
     * Invocado por microservicios cuando modifican datos críticos.
     */
    @PostMapping("/transacciones")
    public ResponseEntity<AuditoriaTransaccionalDTO> registrarCambio(
            @Valid @RequestBody AuditoriaTransaccionalRequestDTO request) {

        log.info("POST /transacciones — entidad={}, id={}", request.entidadAfectada(), request.entidadId());
        AuditoriaTransaccionalDTO creado = servicioSeguridad.registrarCambio(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    /**
     * Historial de cambios transaccionales con filtros opcionales.
     */
    @GetMapping("/transacciones")
    public ResponseEntity<Page<AuditoriaTransaccionalDTO>> listarTransacciones(
            @RequestParam(required = false) String servicioOrigen,
            @RequestParam(required = false) LocalDateTime desde,
            @RequestParam(required = false) LocalDateTime hasta,
            @RequestParam(defaultValue = "0")  int pagina,
            @RequestParam(defaultValue = "20") int tamanio) {

        Pageable paginacion = PageRequest.of(
                Math.max(0, pagina),
                Math.min(tamanio, 100)
        );
        return ResponseEntity.ok(
                servicioSeguridad.listarCambiosTransaccionales(servicioOrigen, desde, hasta, paginacion)
        );
    }
}
