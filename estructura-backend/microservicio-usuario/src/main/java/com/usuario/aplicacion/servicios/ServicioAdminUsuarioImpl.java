package com.usuario.aplicacion.servicios;

import com.libreria.comun.respuesta.Paginacion;
import com.usuario.aplicacion.puertos.IServicioAdminUsuario;
import com.usuario.dominio.entidades.Usuario;
import com.usuario.dominio.especificaciones.UsuarioSpecs;
import com.usuario.dominio.repositorios.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Servicio concreto que implementa las búsquedas administrativas de usuarios.
 * Combina especificaciones dinámicas y encapsula la paginación con el formato estándar de la plataforma.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ServicioAdminUsuarioImpl implements IServicioAdminUsuario {

    private final UsuarioRepository usuarioRepository;

    @Override
    @Transactional(readOnly = true)
    public Paginacion<Usuario> buscarUsuarios(
            Boolean habilitado,
            String rol,
            String texto,
            LocalDateTime desde,
            LocalDateTime hasta,
            int pagina,
            int tamanio) {
        
        log.debug("[ADMIN-SERVICE] Compilando especificaciones de búsqueda dinámica para usuarios. Página: {}, Tamaño: {}", pagina, tamanio);

        Specification<Usuario> spec = Specification.where(UsuarioSpecs.esHabilitado(habilitado))
                .and(UsuarioSpecs.tieneRol(rol))
                .and(UsuarioSpecs.buscarPorTexto(texto))
                .and(UsuarioSpecs.creadoEntre(desde, hasta));

        // Construir la paginación de infraestructura internamente, ordenando de forma descendente por fecha de creación
        Pageable pageable = PageRequest.of(pagina, tamanio, Sort.by("fechaCreacion").descending());
        
        Page<Usuario> page = usuarioRepository.findAll(spec, pageable);
        return Paginacion.desde(page);
    }
}
