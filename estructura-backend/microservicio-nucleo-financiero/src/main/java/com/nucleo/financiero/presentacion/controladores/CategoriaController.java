package com.nucleo.financiero.presentacion.controladores;

import com.nucleo.financiero.aplicacion.dtos.CategoriaDTO;
import com.nucleo.financiero.aplicacion.dtos.CategoriaRequestDTO;
import com.nucleo.financiero.aplicacion.servicios.CategoriaService;
import com.nucleo.financiero.dominio.entidades.Categoria.TipoMovimiento;
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
    public ResponseEntity<CategoriaDTO> crear(@Valid @RequestBody CategoriaRequestDTO request) {
        log.info("Creando nueva categoría: {}", request.nombre());
        return ResponseEntity.status(HttpStatus.CREATED).body(categoriaService.crear(request));
    }

    @GetMapping
    public ResponseEntity<List<CategoriaDTO>> listar(@RequestParam(required = false) TipoMovimiento tipo) {
        return tipo != null 
                ? ResponseEntity.ok(categoriaService.listarPorTipo(tipo)) 
                : ResponseEntity.ok(categoriaService.listarTodas());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CategoriaDTO> obtener(@PathVariable UUID id) {
        return ResponseEntity.ok(categoriaService.obtenerPorId(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CategoriaDTO> actualizar(@PathVariable UUID id, @Valid @RequestBody CategoriaRequestDTO request) {
        return ResponseEntity.ok(categoriaService.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable UUID id) {
        categoriaService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}