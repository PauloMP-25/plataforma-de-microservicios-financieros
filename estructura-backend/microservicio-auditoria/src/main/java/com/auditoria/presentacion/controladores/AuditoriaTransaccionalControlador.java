package com.auditoria.presentacion.controladores;

import com.auditoria.aplicacion.servicios.ServicioAuditoriaTransaccional;
import com.libreria.comun.dtos.EventoTransaccionalDTO;
import com.libreria.comun.respuesta.Paginacion;
import com.libreria.comun.respuesta.ResultadoApi;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Controlador para la consulta de auditorías de cambios transaccionales.
 * <p>
 * Proporciona visibilidad sobre la evolución de los datos en el sistema,
 * permitiendo auditorías técnicas y de negocio.
 * </p>
 * 
 * @author Paulo Moron
 */
@RestController
@RequestMapping("/api/v1/auditoria/transacciones")
@RequiredArgsConstructor
public class AuditoriaTransaccionalControlador {

    private final ServicioAuditoriaTransaccional servicio;

    /**
     * Obtiene el historial de cambios realizados por un usuario específico.
     * 
     * @param usuarioId UUID del usuario.
     * @param pagina Índice de página.
     * @param tamanio Tamaño de página.
     * @return {@link ResultadoApi} con lista paginada de eventos.
     */
    @GetMapping("/usuario/{usuarioId}")
    public ResponseEntity<ResultadoApi<List<EventoTransaccionalDTO>>> listarPorUsuario(
            @PathVariable UUID usuarioId,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamanio) {

        Page<EventoTransaccionalDTO> resultado = servicio.listarPorUsuario(
                usuarioId, PageRequest.of(pagina, Math.min(tamanio, 100)));

        Paginacion<EventoTransaccionalDTO> paginacion = Paginacion.desde(resultado);

        return ResponseEntity.ok(ResultadoApi.exito(
                paginacion.contenido(), 
                "Historial transaccional del usuario recuperado.", 
                paginacion)
        );
    }

    /**
     * Búsqueda avanzada de auditoría transaccional con filtros de servicio y fechas.
     * 
     * @param servicioOrigen Nombre del microservicio.
     * @param desde Fecha de inicio de búsqueda.
     * @param hasta Fecha de fin de búsqueda.
     * @param pagina Índice de página.
     * @return {@link ResultadoApi} con los resultados filtrados.
     */
    @GetMapping("/busqueda")
    public ResponseEntity<ResultadoApi<List<EventoTransaccionalDTO>>> buscar(
            @RequestParam(required = false) String servicioOrigen,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta,
            @RequestParam(defaultValue = "0") int pagina) {

        Page<EventoTransaccionalDTO> resultado = servicio.buscarConFiltros(
                servicioOrigen, desde, hasta, PageRequest.of(pagina, 20));

        Paginacion<EventoTransaccionalDTO> paginacion = Paginacion.desde(resultado);

        return ResponseEntity.ok(ResultadoApi.exito(
                paginacion.contenido(), 
                "Búsqueda transaccional finalizada con éxito.", 
                paginacion)
        );
    }
}

