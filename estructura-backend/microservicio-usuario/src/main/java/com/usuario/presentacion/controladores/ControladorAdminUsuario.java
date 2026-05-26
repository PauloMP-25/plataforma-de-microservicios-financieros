package com.usuario.presentacion.controladores;

import com.libreria.comun.respuesta.Paginacion;
import com.libreria.comun.respuesta.ResultadoApi;
import com.usuario.aplicacion.puertos.IServicioAdminUsuario;
import com.usuario.dominio.entidades.Usuario;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Controlador REST que expone endpoints administrativos de gestión de usuarios.
 * Protegido estrictamente a nivel de método y de clase para perfiles con rol ADMIN.
 * Utiliza variables de tipo primitivo para la paginación para mantener un diseño limpio y portable.
 */
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Administración (Admin)", description = "Controlador con privilegios elevados para la supervisión, auditoría y administración global de cuentas de usuario en LUKA.")
public class ControladorAdminUsuario {

    private final IServicioAdminUsuario servicioAdmin;

    /**
     * Endpoint para la búsqueda y filtrado dinámico de usuarios de forma paginada.
     * Retorna la estructura estandarizada de Paginacion de la librería común.
     * Solo accesible por usuarios con el rol ROLE_ADMIN.
     */
    @GetMapping("/usuarios")
    @Operation(summary = "Búsqueda Dinámica Paginada de Usuarios", description = "Busca, filtra y pagina usuarios de la plataforma por estado habilitado/deshabilitado, rol (ROLE_FREE, ROLE_PREMIUM, ROLE_ADMIN), coincidencias de texto en nombres de usuario/correo, y rangos de fecha de creación. Requiere privilegios elevados de ROLE_ADMIN.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Listado de usuarios recuperado exitosamente con metadatos de paginación."),
            @ApiResponse(responseCode = "401", description = "No autorizado. Token JWT inválido, expirado o ausente."),
            @ApiResponse(responseCode = "403", description = "Acceso denegado. Privilegios insuficientes (Requiere ROLE_ADMIN).")
    })
    public ResponseEntity<ResultadoApi<List<Usuario>>> buscarUsuarios(
            @RequestParam(required = false) @Parameter(description = "Filtro de estado: true para habilitados, false para deshabilitados.") Boolean habilitado,
            @RequestParam(required = false) @Parameter(description = "Filtrar por rol exacto.", example = "ROLE_FREE") String rol,
            @RequestParam(required = false) @Parameter(description = "Texto parcial para buscar coincidencias en nombre de usuario o correo electrónico.", example = "paulo") String texto,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) @Parameter(description = "Fecha y hora inicial del rango de creación (ISO 8601).", example = "2026-01-01T00:00:00") LocalDateTime desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) @Parameter(description = "Fecha y hora final del rango de creación (ISO 8601).", example = "2026-12-31T23:59:59") LocalDateTime hasta,
            @RequestParam(defaultValue = "0") @Parameter(description = "Índice de la página a recuperar (0-indexed).", example = "0") int pagina,
            @RequestParam(defaultValue = "10") @Parameter(description = "Tamaño de elementos por página.", example = "10") int tamanio) {

        log.info("[ADMIN-API] Solicitud de búsqueda de usuarios. Filtros: habilitado={}, rol={}, texto={}, desde={}, hasta={}, pagina={}, tamanio={}",
                habilitado, rol, texto, desde, hasta, pagina, tamanio);

        Paginacion<Usuario> paginacion = servicioAdmin.buscarUsuarios(habilitado, rol, texto, desde, hasta, pagina, tamanio);
        
        return ResponseEntity.ok(ResultadoApi.exito(paginacion.contenido(), "Listado de usuarios recuperado exitosamente.", paginacion));
    }
}
