package com.nucleo.financiero.presentacion.controladores;

import com.nucleo.financiero.aplicacion.dtos.CategoriaDTO;
import com.nucleo.financiero.aplicacion.dtos.CategoriaRequestDTO;
import com.nucleo.financiero.aplicacion.dtos.ErrorApi;
import com.nucleo.financiero.aplicacion.servicios.CategoriaService;
import com.nucleo.financiero.dominio.entidades.Categoria.TipoMovimiento;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/financiero/categorias")
@RequiredArgsConstructor
@Slf4j
public class CategoriaController {

    private final CategoriaService categoriaService;

    @PostMapping
    public ResponseEntity<?> crear(@Valid @RequestBody CategoriaRequestDTO request,
                                   HttpServletRequest httpRequest) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(categoriaService.crear(request));
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ErrorApi.of(409, "CONFLICTO", ex.getMessage(), httpRequest.getRequestURI()));
        }
    }

    @GetMapping
    public ResponseEntity<List<CategoriaDTO>> listar(
            @RequestParam(required = false) TipoMovimiento tipo) {
        if (tipo != null) return ResponseEntity.ok(categoriaService.listarPorTipo(tipo));
        return ResponseEntity.ok(categoriaService.listarTodas());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> obtener(@PathVariable UUID id, HttpServletRequest httpRequest) {
        try {
            return ResponseEntity.ok(categoriaService.obtenerPorId(id));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ErrorApi.of(404, "NO_ENCONTRADO", ex.getMessage(), httpRequest.getRequestURI()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(@PathVariable UUID id,
                                        @Valid @RequestBody CategoriaRequestDTO request,
                                        HttpServletRequest httpRequest) {
        try {
            return ResponseEntity.ok(categoriaService.actualizar(id, request));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ErrorApi.of(404, "NO_ENCONTRADO", ex.getMessage(), httpRequest.getRequestURI()));
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ErrorApi.of(409, "CONFLICTO", ex.getMessage(), httpRequest.getRequestURI()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable UUID id, HttpServletRequest httpRequest) {
        try {
            categoriaService.eliminar(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ErrorApi.of(404, "NO_ENCONTRADO", ex.getMessage(), httpRequest.getRequestURI()));
        }
    }
}
