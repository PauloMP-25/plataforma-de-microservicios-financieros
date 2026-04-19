package com.nucleo.financiero.aplicacion.servicios;

import com.nucleo.financiero.aplicacion.dtos.CategoriaDTO;
import com.nucleo.financiero.aplicacion.dtos.CategoriaRequestDTO;
import com.nucleo.financiero.dominio.entidades.Categoria;
import com.nucleo.financiero.dominio.entidades.Categoria.TipoMovimiento;
import com.nucleo.financiero.dominio.repositorios.CategoriaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class CategoriaService {

    private final CategoriaRepository categoriaRepository;

    @Transactional
    public CategoriaDTO crear(CategoriaRequestDTO request) {
        if (categoriaRepository.existsByNombreIgnoreCase(request.nombre())) {
            throw new IllegalStateException(
                String.format("Ya existe una categoría con el nombre '%s'.", request.nombre()));
        }
        Categoria nueva = Categoria.builder()
                .nombre(request.nombre())
                .descripcion(request.descripcion())
                .icono(request.icono())
                .tipo(request.tipo())
                .build();
        Categoria guardada = categoriaRepository.save(nueva);
        log.info("Categoría creada: '{}' ({})", guardada.getNombre(), guardada.getTipo());
        return CategoriaDTO.desde(guardada);
    }

    @Transactional(readOnly = true)
    public List<CategoriaDTO> listarTodas() {
        return categoriaRepository.findAll()
                .stream()
                .map(CategoriaDTO::desde)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CategoriaDTO> listarPorTipo(TipoMovimiento tipo) {
        return categoriaRepository.findByTipo(tipo)
                .stream()
                .map(CategoriaDTO::desde)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CategoriaDTO obtenerPorId(UUID id) {
        return categoriaRepository.findById(id)
                .map(CategoriaDTO::desde)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Categoría no encontrada con ID: " + id));
    }

    @Transactional
    public CategoriaDTO actualizar(UUID id, CategoriaRequestDTO request) {
        Categoria existente = categoriaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Categoría no encontrada con ID: " + id));

        categoriaRepository.findByNombreIgnoreCase(request.nombre())
                .filter(c -> !c.getId().equals(id))
                .ifPresent(c -> {
                    throw new IllegalStateException(
                        String.format("Ya existe otra categoría con el nombre '%s'.", request.nombre()));
                });

        existente.setNombre(request.nombre());
        existente.setDescripcion(request.descripcion());
        existente.setIcono(request.icono());
        existente.setTipo(request.tipo());

        Categoria actualizada = categoriaRepository.save(existente);
        log.info("Categoría actualizada: '{}' ({})", actualizada.getNombre(), actualizada.getId());
        return CategoriaDTO.desde(actualizada);
    }

    @Transactional
    public void eliminar(UUID id) {
        if (!categoriaRepository.existsById(id)) {
            throw new IllegalArgumentException("Categoría no encontrada con ID: " + id);
        }
        categoriaRepository.deleteById(id);
        log.info("Categoría eliminada: {}", id);
    }
}
