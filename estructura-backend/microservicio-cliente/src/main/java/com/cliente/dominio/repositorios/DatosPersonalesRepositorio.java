package com.cliente.dominio.repositorios;

import com.cliente.dominio.entidades.DatosPersonales;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface DatosPersonalesRepositorio extends JpaRepository<DatosPersonales, UUID> {

    Optional<DatosPersonales> findByUsuarioId(UUID usuarioId);

    boolean existsByUsuarioId(UUID usuarioId);

    boolean existsByDni(String dni);

    Optional<DatosPersonales> findByDni(String dni);
}
