package com.nucleo.financiero.dominio.repositorios;

import com.nucleo.financiero.dominio.entidades.Categoria;
import com.nucleo.financiero.dominio.entidades.Categoria.TipoMovimiento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio para la persistencia de categorías.
 * Proporciona métodos para búsqueda por nombre (case-insensitive) y filtrado por tipo.
 * 
 * @author Luka-Dev-Backend
 */
@Repository
public interface CategoriaRepository extends JpaRepository<Categoria, UUID> {

    Optional<Categoria> findByNombreIgnoreCase(String nombre);

    boolean existsByNombreIgnoreCase(String nombre);

    List<Categoria> findByTipo(TipoMovimiento tipo);
}
