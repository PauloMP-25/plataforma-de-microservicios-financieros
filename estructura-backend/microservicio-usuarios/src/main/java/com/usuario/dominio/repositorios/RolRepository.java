package com.usuario.dominio.repositorios;

import com.usuario.dominio.entidades.Rol;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RolRepository extends JpaRepository<Rol, Long> {
    Optional<Rol> findByNombre(String name);
    public boolean existsByNombre(String name);
}
