package com.nucleo.financiero.presentacion.controladores;

import com.nucleo.financiero.aplicacion.dtos.transacciones.*;
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
@RequestMapping("/api/v1/transacciones")
@RequiredArgsConstructor
@Slf4j
@Validated
public class TransaccionController {

    private final TransaccionService transaccionService;

    @PostMapping
    public ResponseEntity<TransaccionDTO> registrar(@Valid @RequestBody TransaccionRequestDTO request, HttpServletRequest httpRequest) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(transaccionService.registrar(request, obtenerIp(httpRequest)));
    }

    @PostMapping("/lote")
    public ResponseEntity<List<TransaccionDTO>> registrarLote(
            @Valid @RequestBody List<@Valid TransaccionRequestDTO> solicitudes,
            HttpServletRequest httpRequest) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(transaccionService.registrarLote(solicitudes, obtenerIp(httpRequest)));
    }

    @GetMapping("/historial")
    public ResponseEntity<Page<TransaccionDTO>> listarHistorial(
            @RequestParam UUID usuarioId,
            @RequestParam(required = false) TipoMovimiento tipo,
            @RequestParam(required = false) UUID categoriaId,
            @RequestParam(required = false) Integer mes,
            @RequestParam(required = false) Integer anio,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamanio,
            HttpServletRequest httpRequest) {

        Pageable paginacion = PageRequest.of(Math.max(0, pagina), Math.min(tamanio, 100));
        return ResponseEntity.ok(transaccionService.listarHistorial(usuarioId, tipo, categoriaId, mes, anio, paginacion, obtenerIp(httpRequest)));
    }

    @GetMapping("/resumen")
    public ResponseEntity<ResumenFinancieroDTO> obtenerResumen(
            @RequestParam UUID usuarioId,
            @RequestParam(required = false) Integer mes,
            @RequestParam(required = false) Integer anio,
            HttpServletRequest httpRequest) {
        return ResponseEntity.ok(transaccionService.obtenerResumen(usuarioId, mes, anio, obtenerIp(httpRequest)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TransaccionDTO> obtenerPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(transaccionService.obtenerPorId(id));
    }

    // =========================================================================
    // HELPERS
    // =========================================================================
    private String obtenerIp(HttpServletRequest request) {
        String ip = request.getRemoteAddr();
        return ip;
    }
}
