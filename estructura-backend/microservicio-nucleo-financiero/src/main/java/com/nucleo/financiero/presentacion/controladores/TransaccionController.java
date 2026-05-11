package com.nucleo.financiero.presentacion.controladores;

import com.libreria.comun.respuesta.Paginacion;
import com.libreria.comun.respuesta.ResultadoApi;
import com.nucleo.financiero.aplicacion.dtos.transacciones.*;
import com.nucleo.financiero.aplicacion.servicios.ITransaccionService;
import com.nucleo.financiero.dominio.entidades.Categoria.TipoMovimiento;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * Controlador REST para el registro y gestión de transacciones financieras.
 * <p>
 * Centraliza la lógica de ingresos y egresos manuales, permitiendo tanto
 * registros
 * individuales como en lote. Se comunica a través del contrato
 * {@link ITransaccionService}.
 * </p>
 *
 * @author Luka-Dev-Backend
 * @version 1.2.2
 */
@RestController
@RequestMapping("/api/v1/transacciones")
@RequiredArgsConstructor
@Slf4j
@Validated
public class TransaccionController {

    private final ITransaccionService transaccionService;

    /**
     * Registra un movimiento financiero individual (Ingreso/Egreso).
     * 
     * @param request     Datos de la transacción.
     * @param httpRequest Datos de la petición para auditoría.
     * @return ResponseEntity con la transacción registrada.
     */
    @PostMapping
    public ResponseEntity<ResultadoApi<RespuestaTransaccion>> registrar(
            @Valid @RequestBody SolicitudTransaccion request,
            HttpServletRequest httpRequest) {

        RespuestaTransaccion respuesta = transaccionService.registrar(request, obtenerIp(httpRequest));
        return ResponseEntity.status(201).body(ResultadoApi.creado(respuesta, "Transacción registrada con éxito"));
    }

    /**
     * Registra un conjunto de transacciones en una sola operación (Lote).
     * <p>
     * Optimizado para importaciones masivas o sincronizaciones iniciales.
     * </p>
     * 
     * @param solicitudes Lista de transacciones a registrar.
     * @param httpRequest Datos de la petición para auditoría.
     * @return ResponseEntity con el listado de transacciones procesadas.
     */
    @PostMapping("/lote")
    public ResponseEntity<ResultadoApi<List<RespuestaTransaccion>>> registrarLote(
            @Valid @RequestBody List<@Valid SolicitudTransaccion> solicitudes,
            HttpServletRequest httpRequest) {

        List<RespuestaTransaccion> respuesta = transaccionService.registrarLote(solicitudes, obtenerIp(httpRequest));
        return ResponseEntity.status(201).body(ResultadoApi.creado(respuesta, "Lote de transacciones procesado"));
    }

    /**
     * Consulta el historial paginado de transacciones con filtros dinámicos.
     * 
     * @param usuarioId   ID del propietario de las transacciones.
     * @param tipo        Filtro por INGRESO o EGRESO.
     * @param categoriaId Filtro por categoría específica.
     * @param desde       Fecha inicial del rango.
     * @param hasta       Fecha final del rango.
     * @param pagina      Número de página (0-based).
     * @param tamanio     Elementos por página.
     * @param httpRequest Datos de la petición.
     * @return ResponseEntity con el resultado paginado estandarizado.
     */
    @GetMapping("/historial")
    public ResponseEntity<ResultadoApi<List<RespuestaTransaccion>>> listarHistorial(
            @RequestParam UUID usuarioId,
            @RequestParam(required = false) TipoMovimiento tipo,
            @RequestParam(required = false) UUID categoriaId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamanio,
            HttpServletRequest httpRequest) {

        Pageable paginacionSpring = PageRequest.of(pagina, tamanio, Sort.by("fechaTransaccion").descending());
        Page<RespuestaTransaccion> page = transaccionService.listarHistorial(
                usuarioId, tipo, categoriaId, desde, hasta, paginacionSpring, obtenerIp(httpRequest));

        return ResponseEntity.ok(ResultadoApi.exito(
                page.getContent(),
                "Historial recuperado",
                Paginacion.desde(page)));
    }

    /**
     * Obtiene un resumen consolidado de las finanzas en un periodo determinado.
     * 
     * @param usuarioId   ID del usuario.
     * @param mes         Mes del resumen (1-12).
     * @param anio        Año del resumen.
     * @param httpRequest Datos de la petición.
     * @return ResponseEntity con el DTO de resumen financiero.
     */
    @GetMapping("/resumen")
    public ResponseEntity<ResultadoApi<ResumenFinancieroDTO>> obtenerResumen(
            @RequestParam UUID usuarioId,
            @RequestParam(required = false) Integer mes,
            @RequestParam(required = false) Integer anio,
            HttpServletRequest httpRequest) {

        ResumenFinancieroDTO resumen = transaccionService.obtenerResumen(usuarioId, mes, anio, obtenerIp(httpRequest));
        return ResponseEntity.ok(ResultadoApi.exito(resumen, "Resumen financiero generado"));
    }

    /**
     * Busca una transacción específica por su identificador único.
     * 
     * @param id UUID de la transacción.
     * @return ResponseEntity con el detalle de la transacción.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ResultadoApi<RespuestaTransaccion>> obtenerPorId(@PathVariable UUID id) {
        RespuestaTransaccion respuesta = transaccionService.obtenerPorId(id);
        return ResponseEntity.ok(ResultadoApi.exito(respuesta));
    }

    private String obtenerIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        return (ip != null) ? ip : request.getRemoteAddr();
    }
}
