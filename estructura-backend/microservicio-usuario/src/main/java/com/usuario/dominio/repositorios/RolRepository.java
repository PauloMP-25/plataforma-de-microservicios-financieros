package com.usuario.dominio.repositorios;

import com.usuario.dominio.entidades.Rol;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RolRepository extends JpaRepository<Rol, UUID> {
    Optional<Rol> findByNombre(String name);
    
    boolean existsByNombre(String name);
}
