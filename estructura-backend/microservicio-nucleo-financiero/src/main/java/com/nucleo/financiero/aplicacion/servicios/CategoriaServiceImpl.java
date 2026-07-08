package com.nucleo.financiero.aplicacion.servicios;

import com.nucleo.financiero.aplicacion.dtos.respuestas.CategoriaDTO;
import com.nucleo.financiero.aplicacion.dtos.solicitudes.CategoriaRequestDTO;
import com.nucleo.financiero.aplicacion.puertos.ICategoriaService;
import com.nucleo.financiero.aplicacion.mappers.CategoriaMapper;
import com.nucleo.financiero.dominio.entidades.Categoria;
import com.nucleo.financiero.dominio.entidades.Categoria.TipoMovimiento;
import com.nucleo.financiero.dominio.repositorios.CategoriaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.libreria.comun.excepciones.ExcepcionRecursoNoEncontrado;
import com.libreria.comun.excepciones.ExcepcionConflicto;
import java.util.List;
import java.util.UUID;

/**
 * Implementación de {@link ICategoriaService} para la gestión de categorías.
 * Aplica lógica de negocio y persistencia para el dominio financiero.
 *
 * @version 1.2.0
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class CategoriaServiceImpl implements ICategoriaService {

    private final CategoriaRepository categoriaRepository;
    private final CategoriaMapper categoriaMapper;

    @Override
    @Transactional
    public CategoriaDTO crear(CategoriaRequestDTO request) {
        if (categoriaRepository.existsByNombreIgnoreCase(request.nombre())) {
            throw new ExcepcionConflicto("nombre", request.nombre());
        }
        Categoria nueva = Categoria.builder()
                .nombre(request.nombre())
                .descripcion(request.descripcion())
                .icono(request.icono())
                .tipo(request.tipo())
                .build();
        Categoria guardada = categoriaRepository.save(nueva);
        log.info("Categoría creada: '{}' ({})", guardada.getNombre(), guardada.getTipo());
        return categoriaMapper.aDto(guardada);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoriaDTO> listarTodas() {
        return categoriaRepository.findAll()
                .stream()
                .map(categoriaMapper::aDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoriaDTO> listarPorTipo(TipoMovimiento tipo) {
        return categoriaRepository.findByTipo(tipo)
                .stream()
                .map(categoriaMapper::aDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CategoriaDTO obtenerPorId(UUID id) {
        return categoriaRepository.findById(id)
                .map(categoriaMapper::aDto)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("Categoria", id));
    }

    @Override
    @Transactional
    public CategoriaDTO actualizar(UUID id, CategoriaRequestDTO request) {
        Categoria existente = categoriaRepository.findById(id)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("Categoria", id));

        categoriaRepository.findByNombreIgnoreCase(request.nombre())
                .filter(c -> !c.getId().equals(id))
                .ifPresent(c -> {
                    throw new ExcepcionConflicto("nombre", request.nombre());
                });

        existente.setNombre(request.nombre());
        existente.setDescripcion(request.descripcion());
        existente.setIcono(request.icono());
        existente.setTipo(request.tipo());

        Categoria actualizada = categoriaRepository.save(existente);
        log.info("Categoría actualizada: '{}' ({})", actualizada.getNombre(), actualizada.getId());
        return categoriaMapper.aDto(actualizada);
    }

    @Override
    @Transactional
    public void eliminar(UUID id) {
        if (!categoriaRepository.existsById(id)) {
            throw new ExcepcionRecursoNoEncontrado("Categoria", id);
        }
        categoriaRepository.deleteById(id);
        log.info("Categoría eliminada: {}", id);
    }
}
