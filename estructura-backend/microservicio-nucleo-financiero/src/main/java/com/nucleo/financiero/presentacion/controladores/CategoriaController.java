package com.nucleo.financiero.presentacion.controladores;

import com.libreria.comun.respuesta.ResultadoApi;
import com.nucleo.financiero.aplicacion.dtos.transacciones.CategoriaDTO;
import com.nucleo.financiero.aplicacion.dtos.transacciones.CategoriaRequestDTO;
import com.nucleo.financiero.aplicacion.servicios.ICategoriaService;
import com.nucleo.financiero.dominio.entidades.Categoria.TipoMovimiento;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

/**
 * Controlador REST para la gestión de categorías financieras.
 * <p>
 * Estandarizado con {@link ResultadoApi} para proporcionar respuestas consistentes
 * a los consumidores del frontend y otros microservicios.
 * </p>
 *
 * @author Luka-Dev-Backend
 * @version 1.2.1
 */
@RestController
@RequestMapping("/api/v1/financiero/categorias")
@RequiredArgsConstructor
@Slf4j
public class CategoriaController {

    private final ICategoriaService categoriaService;

    /**
     * Registra una nueva categoría en el sistema.
     * 
     * @param request Datos de la categoría validados.
     * @return ResponseEntity con la categoría creada.
     */
    @PostMapping
    public ResponseEntity<ResultadoApi<CategoriaDTO>> crear(@Valid @RequestBody CategoriaRequestDTO request) {
        log.info("REST request para crear categoría: {}", request.nombre());
        CategoriaDTO dto = categoriaService.crear(request);
        return ResponseEntity.status(201).body(ResultadoApi.creado(dto, "Categoría creada correctamente"));
    }

    /**
     * Lista todas las categorías, permitiendo filtrado opcional por tipo de movimiento.
     * 
     * @param tipo Tipo de movimiento (INGRESO o EGRESO). Opcional.
     * @return ResponseEntity con la lista de categorías encontradas.
     */
    @GetMapping
    public ResponseEntity<ResultadoApi<List<CategoriaDTO>>> listar(@RequestParam(required = false) TipoMovimiento tipo) {
        List<CategoriaDTO> lista = (tipo != null) 
                ? categoriaService.listarPorTipo(tipo) 
                : categoriaService.listarTodas();
        // Se utiliza la sobrecarga de exito(T datos) para listas
        return ResponseEntity.ok(ResultadoApi.exito(lista));
    }

    /**
     * Recupera el detalle de una categoría específica por su identificador único.
     * 
     * @param id UUID de la categoría.
     * @return ResponseEntity con el DTO de la categoría.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ResultadoApi<CategoriaDTO>> obtener(@PathVariable UUID id) {
        CategoriaDTO dto = categoriaService.obtenerPorId(id);
        return ResponseEntity.ok(ResultadoApi.exito(dto));
    }

    /**
     * Actualiza los atributos de una categoría existente.
     * 
     * @param id UUID de la categoría a modificar.
     * @param request Nuevos datos de la categoría.
     * @return ResponseEntity con el estado actualizado.
     */
    @PutMapping("/{id}")
    public ResponseEntity<ResultadoApi<CategoriaDTO>> actualizar(@PathVariable UUID id, @Valid @RequestBody CategoriaRequestDTO request) {
        CategoriaDTO dto = categoriaService.actualizar(id, request);
        return ResponseEntity.ok(ResultadoApi.exito(dto, "Categoría actualizada correctamente"));
    }

    /**
     * Elimina una categoría del sistema.
     * 
     * @param id UUID de la categoría a eliminar.
     * @return ResponseEntity confirmando la eliminación.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ResultadoApi<Void>> eliminar(@PathVariable UUID id) {
        categoriaService.eliminar(id);
        return ResponseEntity.ok(ResultadoApi.sinContenido("Categoría eliminada correctamente"));
    }
}