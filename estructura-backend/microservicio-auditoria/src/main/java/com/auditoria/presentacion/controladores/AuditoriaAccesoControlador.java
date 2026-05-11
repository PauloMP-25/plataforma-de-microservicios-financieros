package com.auditoria.presentacion.controladores;

import com.auditoria.aplicacion.servicios.ServicioAuditoriaAcceso;
import com.libreria.comun.dtos.EventoAccesoDTO;
import com.libreria.comun.respuesta.Paginacion;
import com.libreria.comun.respuesta.ResultadoApi;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controlador de presentación para la gestión de auditorías de acceso.
 * <p>
 * Expone endpoints para la consulta de registros de inicio de sesión y actividad,
 * integrando el estándar de respuestas {@link ResultadoApi} y metadatos de {@link Paginacion}.
 * </p>
 * 
 * @author Paulo Moron
 * @version 1.3
 * @since 2026-05
 */
@RestController
@RequestMapping("/api/auditoria/accesos")
@RequiredArgsConstructor
public class AuditoriaAccesoControlador {

    private final ServicioAuditoriaAcceso servicio;

    /**
     * Recupera la lista paginada de todos los eventos de acceso registrados en el sistema.
     * 
     * @param pagina  Número de página (0 por defecto).
     * @param tamanio Cantidad de registros (20 por defecto, máx 100).
     * @return {@link ResponseEntity} con el {@link ResultadoApi} y metadatos de paginación.
     */
    @GetMapping
    public ResponseEntity<ResultadoApi<List<EventoAccesoDTO>>> listarTodo(
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamanio) {

        int tamanioSeguro = Math.min(tamanio, 100);
        Page<EventoAccesoDTO> resultadoPage = servicio.listarTodo(PageRequest.of(pagina, tamanioSeguro));
        
        Paginacion<EventoAccesoDTO> metadata = Paginacion.desde(resultadoPage);

        return ResponseEntity.ok(
            ResultadoApi.exito(
                metadata.contenido(), 
                "Catálogo de accesos recuperado exitosamente.", 
                metadata
            )
        );
    }

    /**
     * Busca los registros de acceso asociados a un usuario específico.
     * 
     * @param usuarioId Identificador único del usuario (UUID).
     * @param pagina    Número de página solicitado.
     * @return {@link ResponseEntity} con el resultado paginado para el usuario indicado.
     */
    @GetMapping("/usuario/{usuarioId}")
    public ResponseEntity<ResultadoApi<List<EventoAccesoDTO>>> obtenerPorUsuario(
            @PathVariable UUID usuarioId,
            @RequestParam(defaultValue = "0") int pagina) {

        Page<EventoAccesoDTO> resultadoPage = servicio.listarPorUsuario(usuarioId, PageRequest.of(pagina, 20));
        
        Paginacion<EventoAccesoDTO> metadata = Paginacion.desde(resultadoPage);
        String mensaje = String.format("Registros de acceso para el usuario %s recuperados.", usuarioId);

        return ResponseEntity.ok(
            ResultadoApi.exito(metadata.contenido(), mensaje, metadata)
        );
    }
}