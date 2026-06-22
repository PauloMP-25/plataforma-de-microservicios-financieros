package com.cliente.dominio.repositorios;

import com.cliente.dominio.entidades.MetaAhorro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Repository
public interface MetaAhorroRepositorio extends JpaRepository<MetaAhorro, UUID>, JpaSpecificationExecutor<MetaAhorro> {

    Page<MetaAhorro> findByUsuarioIdAndActivaTrueOrderByFechaCreacionDesc(UUID usuarioId, Pageable pageable);
    
    // Para listarInterno (sin paginación)
    List<MetaAhorro> findByUsuarioIdAndActivaTrueOrderByFechaCreacionDesc(UUID usuarioId);

    List<MetaAhorro> findByUsuarioIdAndCompletadaAndActivaTrueOrderByFechaCreacionDesc(UUID usuarioId, Boolean completada);

    long countByUsuarioIdAndCompletadaAndActivaTrue(UUID usuarioId, Boolean completada);

    Page<MetaAhorro> findByUsuarioIdAndCompletadaFalseAndActivaTrue(UUID usuarioId, Pageable pageable);
}
