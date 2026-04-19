package com.nucleo.financiero.presentacion.controladores;

import com.nucleo.financiero.aplicacion.dtos.*;
import com.nucleo.financiero.aplicacion.servicios.TransaccionService;
import com.nucleo.financiero.dominio.entidades.Categoria.TipoMovimiento;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/financiero/transacciones")
@RequiredArgsConstructor
@Slf4j
@Validated
public class TransaccionController {

    private final TransaccionService transaccionService;

    @PostMapping
    public ResponseEntity<?> registrar(@Valid @RequestBody TransaccionRequestDTO request,
                                       HttpServletRequest httpRequest) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(transaccionService.registrar(request));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest()
                    .body(ErrorApi.of(400, "ERROR_VALIDACION", ex.getMessage(), httpRequest.getRequestURI()));
        }
    }

    @PostMapping("/lote")
    public ResponseEntity<?> registrarLote(
            @Valid @RequestBody List<@Valid TransaccionRequestDTO> solicitudes,
            HttpServletRequest httpRequest) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(transaccionService.registrarLote(solicitudes));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest()
                    .body(ErrorApi.of(400, "ERROR_VALIDACION", ex.getMessage(), httpRequest.getRequestURI()));
        }
    }

    @GetMapping("/historial")
    public ResponseEntity<?> listarHistorial(
            @RequestParam UUID usuarioId,
            @RequestParam(required = false) TipoMovimiento tipo,
            @RequestParam(required = false) UUID categoriaId,
            @RequestParam(required = false) String nombreCliente,
            @RequestParam(required = false) Integer mes,
            @RequestParam(required = false) Integer anio,
            @RequestParam(defaultValue = "0")  int pagina,
            @RequestParam(defaultValue = "20") int tamanio,
            HttpServletRequest httpRequest) {
        Pageable paginacion = PageRequest.of(Math.max(0, pagina), Math.min(tamanio, 100));
        try {
            Page<TransaccionDTO> resultado = transaccionService
                    .listarHistorial(usuarioId, tipo, categoriaId, nombreCliente, mes, anio, paginacion);
            return ResponseEntity.ok(resultado);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest()
                    .body(ErrorApi.of(400, "ERROR_VALIDACION", ex.getMessage(), httpRequest.getRequestURI()));
        }
    }

    @GetMapping("/resumen")
    public ResponseEntity<?> obtenerResumen(
            @RequestParam UUID usuarioId,
            @RequestParam(required = false) Integer mes,
            @RequestParam(required = false) Integer anio,
            HttpServletRequest httpRequest) {
        try {
            return ResponseEntity.ok(transaccionService.obtenerResumen(usuarioId, mes, anio));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest()
                    .body(ErrorApi.of(400, "ERROR_VALIDACION", ex.getMessage(), httpRequest.getRequestURI()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> obtenerPorId(@PathVariable UUID id, HttpServletRequest httpRequest) {
        try {
            return ResponseEntity.ok(transaccionService.obtenerPorId(id));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ErrorApi.of(404, "NO_ENCONTRADO", ex.getMessage(), httpRequest.getRequestURI()));
        }
    }
}
