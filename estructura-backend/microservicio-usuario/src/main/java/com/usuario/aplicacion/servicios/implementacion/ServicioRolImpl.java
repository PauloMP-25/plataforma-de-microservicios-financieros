package com.usuario.aplicacion.servicios.implementacion;

import com.usuario.aplicacion.servicios.IServicioRol;
import com.usuario.dominio.entidades.Rol;
import com.usuario.dominio.entidades.Usuario;
import com.usuario.dominio.repositorios.RolRepository;
import com.usuario.dominio.repositorios.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Implementación de la lógica de gestión de planes y roles sincronizados.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ServicioRolImpl implements IServicioRol {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;

    @SuppressWarnings("null")
    @Override
    @Transactional
    public void actualizarPlanUsuario(UUID usuarioId, String nuevoPlan, LocalDateTime fechaVencimiento) {
        log.info("[SUSCRIPCION] Sincronizando plan y rol para usuario {}: {}", usuarioId, nuevoPlan);

        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado: " + usuarioId));

        // 1. Actualizar metadatos de vigencia
        usuario.setPlanActual(nuevoPlan);
        usuario.setFechaVencimientoPlan(fechaVencimiento);

        // 2. Sincronizar Roles de Seguridad
        // Regla: Un usuario solo tiene UN rol de plan activo (PREMIUM, PRO, o FREE)
        // Eliminamos roles previos de planes para evitar duplicidad
        usuario.getRoles().removeIf(rol -> 
            rol.getNombre().equals("ROLE_FREE") || 
            rol.getNombre().equals("ROLE_PREMIUM") || 
            rol.getNombre().equals("ROLE_PRO")
        );

        // Asignar el nuevo rol correspondiente
        String nombreRol = "ROLE_" + nuevoPlan.toUpperCase();
        Rol nuevoRol = rolRepository.findByNombre(nombreRol)
                .orElseThrow(() -> new RuntimeException("El rol " + nombreRol + " no existe en la base de datos."));

        usuario.getRoles().add(nuevoRol);

        usuarioRepository.save(usuario);
        log.info("[SUSCRIPCION-EXITO] Usuario {} sincronizado con plan {} y rol {}", 
            usuario.getCorreo(), nuevoPlan, nombreRol);
    }
}
