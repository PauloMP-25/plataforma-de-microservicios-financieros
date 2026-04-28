package com.cliente.dominio.repositorios;

import com.cliente.dominio.entidades.PerfilFinanciero;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PerfilFinancieroRepositorio extends JpaRepository<PerfilFinanciero, UUID> {

    Optional<PerfilFinanciero> findByUsuarioId(UUID usuarioId);

    boolean existsByUsuarioId(UUID usuarioId);
}
