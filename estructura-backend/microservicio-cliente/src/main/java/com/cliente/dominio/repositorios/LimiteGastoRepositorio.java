package com.cliente.dominio.repositorios;

import com.cliente.dominio.entidades.LimiteGasto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LimiteGastoRepositorio extends JpaRepository<LimiteGasto, UUID> {

    List<LimiteGasto> findByUsuarioIdOrderByCategoriaIdAsc(UUID usuarioId);

    Optional<LimiteGasto> findByUsuarioIdAndCategoriaId(UUID usuarioId, String categoriaId);

    boolean existsByUsuarioIdAndCategoriaId(UUID usuarioId, String categoriaId);

    void deleteByUsuarioIdAndCategoriaId(UUID usuarioId, String categoriaId);
}
