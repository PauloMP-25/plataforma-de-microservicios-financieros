package com.nucleo.financiero.aplicacion.servicios;

import com.nucleo.financiero.aplicacion.dtos.transacciones.CategoriaDTO;
import com.nucleo.financiero.aplicacion.dtos.transacciones.CategoriaRequestDTO;
import com.nucleo.financiero.dominio.entidades.Categoria.TipoMovimiento;
import java.util.List;
import java.util.UUID;

/**
 * Interfaz de servicio para la gestión de categorías financieras.
 * Define el contrato de negocio para el registro y consulta de categorías.
 *
 * @author Luka-Dev-Backend
 * @version 1.1.0
 */
public interface ICategoriaService {

    /**
     * Registra una nueva categoría en el sistema.
     * @param request Datos de la categoría a crear
     * @return DTO de la categoría creada
     * @throws IllegalStateException Si el nombre de la categoría ya existe
     */
    CategoriaDTO crear(CategoriaRequestDTO request);

    /**
     * Lista todas las categorías registradas.
     * @return Lista de DTOs de categorías
     */
    List<CategoriaDTO> listarTodas();

    /**
     * Filtra categorías por tipo de movimiento (INGRESO/EGRESO).
     * @param tipo Tipo de movimiento
     * @return Lista filtrada de DTOs
     */
    List<CategoriaDTO> listarPorTipo(TipoMovimiento tipo);

    /**
     * Obtiene el detalle de una categoría por su ID.
     * @param id Identificador único de la categoría
     * @return DTO de la categoría
     * @throws IllegalArgumentException Si la categoría no existe
     */
    CategoriaDTO obtenerPorId(UUID id);

    /**
     * Actualiza los datos de una categoría existente.
     * @param id Identificador de la categoría
     * @param request Nuevos datos
     * @return DTO actualizado
     */
    CategoriaDTO actualizar(UUID id, CategoriaRequestDTO request);

    /**
     * Elimina una categoría del sistema.
     * @param id Identificador de la categoría
     */
    void eliminar(UUID id);
}
