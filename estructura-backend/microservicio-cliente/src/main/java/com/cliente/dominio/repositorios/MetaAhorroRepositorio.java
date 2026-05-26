package com.cliente.dominio.repositorios;

import com.cliente.dominio.entidades.MetaAhorro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

@Repository
public interface MetaAhorroRepositorio extends JpaRepository<MetaAhorro, UUID>, JpaSpecificationExecutor<MetaAhorro> {

    List<MetaAhorro> findByUsuarioIdOrderByFechaCreacionDesc(UUID usuarioId);

    List<MetaAhorro> findByUsuarioIdAndCompletadaOrderByFechaCreacionDesc(UUID usuarioId, Boolean completada);

    long countByUsuarioIdAndCompletada(UUID usuarioId, Boolean completada);

    @Query("SELECT m FROM MetaAhorro m WHERE m.usuarioId = :usuarioId AND m.completada = false ORDER BY m.fechaLimite ASC NULLS LAST")
    List<MetaAhorro> findMetasActivasOrdenadas(@Param("usuarioId") UUID usuarioId);
}
