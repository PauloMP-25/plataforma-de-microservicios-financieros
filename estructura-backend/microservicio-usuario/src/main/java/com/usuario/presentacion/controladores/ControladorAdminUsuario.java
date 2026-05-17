package com.usuario.presentacion.controladores;

import com.libreria.comun.respuesta.Paginacion;
import com.libreria.comun.respuesta.ResultadoApi;
import com.usuario.aplicacion.puertos.IServicioAdminUsuario;
import com.usuario.dominio.entidades.Usuario;
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
public class ControladorAdminUsuario {

    private final IServicioAdminUsuario servicioAdmin;

    /**
     * Endpoint para la búsqueda y filtrado dinámico de usuarios de forma paginada.
     * Retorna la estructura estandarizada de Paginacion de la librería común.
     * Solo accesible por usuarios con el rol ROLE_ADMIN.
     *
     * @param habilitado Parámetro opcional para filtrar por estado.
     * @param rol        Parámetro opcional para filtrar por rol del usuario.
     * @param texto      Parámetro opcional para buscar coincidencia en usuario o correo.
     * @param desde      Parámetro opcional para rango de fecha de creación inicial.
     * @param hasta      Parámetro opcional para rango de fecha de creación final.
     * @param pagina     Número de página actual (por defecto 0).
     * @param tamanio    Cantidad de registros por página (por defecto 10).
     * @return Respuesta estandarizada con la página de usuarios correspondientes en formato simplificado.
     */
    @GetMapping("/usuarios")
    public ResponseEntity<ResultadoApi<List<Usuario>>> buscarUsuarios(
            @RequestParam(required = false) Boolean habilitado,
            @RequestParam(required = false) String rol,
            @RequestParam(required = false) String texto,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "10") int tamanio) {

        log.info("[ADMIN-API] Solicitud de búsqueda de usuarios. Filtros: habilitado={}, rol={}, texto={}, desde={}, hasta={}, pagina={}, tamanio={}",
                habilitado, rol, texto, desde, hasta, pagina, tamanio);

        Paginacion<Usuario> paginacion = servicioAdmin.buscarUsuarios(habilitado, rol, texto, desde, hasta, pagina, tamanio);
        
        return ResponseEntity.ok(ResultadoApi.exito(paginacion.contenido(), "Listado de usuarios recuperado exitosamente.", paginacion));
    }
}
