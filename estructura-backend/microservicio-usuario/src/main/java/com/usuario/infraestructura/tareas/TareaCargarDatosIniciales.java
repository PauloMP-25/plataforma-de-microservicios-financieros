package com.usuario.infraestructura.configuracion;

import com.usuario.dominio.entidades.Rol;
import com.usuario.dominio.repositorios.RolRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CargadorDatosIniciales implements CommandLineRunner {

    private final RolRepository repositorioRol;

    @Override
    public void run(String... args) {

        crearRolSiNoExiste(Rol.NombreRol.ROLE_FREE);
        crearRolSiNoExiste(Rol.NombreRol.ROLE_PRO);
        crearRolSiNoExiste(Rol.NombreRol.ROLE_PREMIUM);
        crearRolSiNoExiste(Rol.NombreRol.ROLE_ADMIN);
        crearRolSiNoExiste(Rol.NombreRol.ROLE_ADMINISTRADOR);

        log.info("---- VERIFICACIÓN DE ROLES (LUKA V4) COMPLETADA ----");
    }

    private void crearRolSiNoExiste(Rol.NombreRol nombreRol) {
        boolean existe = repositorioRol.existsByNombre(nombreRol.name());
        if (!existe) {
            repositorioRol.save(new Rol(null, nombreRol.name()));
            log.info("Rol creado: {}", nombreRol.name());
        }
    }
}
