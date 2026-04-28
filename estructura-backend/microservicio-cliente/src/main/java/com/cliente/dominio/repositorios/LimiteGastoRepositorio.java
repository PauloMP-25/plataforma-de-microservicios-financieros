package com.cliente.dominio.repositorios;

import com.cliente.dominio.entidades.LimiteGasto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

@Repository
public interface LimiteGastoRepositorio extends JpaRepository<LimiteGasto, UUID> {

    Optional<LimiteGasto> findByUsuarioIdAndActivoTrue(UUID usuarioId);

    @Modifying
    @Query("UPDATE LimiteGastoGlobal l SET l.activo = false WHERE l.usuarioId = :usuarioId")
    void desactivarLimitesAnteriores(UUID usuarioId);

    Optional<LimiteGasto> findByUsuarioIdOrderByFechaCreacionDesc(UUID usuarioId);
}
