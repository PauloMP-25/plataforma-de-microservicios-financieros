package com.usuario.infraestructura.mensajeria;

import com.usuario.dominio.entidades.Rol;
import com.usuario.dominio.entidades.Usuario;
import com.usuario.dominio.repositorios.RolRepository;
import com.usuario.dominio.repositorios.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * Consumidor para eventos de suscripciones, específicamente la expiración de la suscripción de un usuario.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ConsumidorSuscripcionExpirada {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;

    /**
     * Escucha el evento de suscripción expirada.
     * Al recibir el usuarioId, busca al usuario y cambia su rol y plan a FREE.
     */
    @RabbitListener(queues = "cola.usuario.suscripcion.expirada")
    @Transactional
    public void procesarSuscripcionExpirada(String mensajeUsuarioId) {
        log.info("Evento de suscripción expirada recibido para usuario: {}", mensajeUsuarioId);
        try {
            UUID usuarioId = UUID.fromString(mensajeUsuarioId.replace("\"", ""));
            
            Optional<Usuario> usuarioOpt = usuarioRepository.findById(usuarioId);
            if (usuarioOpt.isPresent()) {
                Usuario usuario = usuarioOpt.get();
                
                // Actualizar plan a FREE
                usuario.setPlanActual("FREE");
                
                // Actualizar rol a FREE
                Optional<Rol> rolFreeOpt = rolRepository.findByNombre(Rol.NombreRol.ROLE_FREE.name());
                if (rolFreeOpt.isPresent()) {
                    usuario.getRoles().clear();
                    usuario.getRoles().add(rolFreeOpt.get());
                } else {
                    log.warn("El rol ROLE_FREE no se encontró en la base de datos");
                }
                
                usuarioRepository.save(usuario);
                log.info("Plan y rol del usuario {} actualizados a FREE por expiración de suscripción.", usuarioId);
            } else {
                log.warn("Usuario {} no encontrado para actualizar su suscripción a expirada.", usuarioId);
            }
        } catch (Exception e) {
            log.error("Error al procesar el evento de suscripción expirada para el usuario {}: {}", mensajeUsuarioId, e.getMessage());
        }
    }
}
