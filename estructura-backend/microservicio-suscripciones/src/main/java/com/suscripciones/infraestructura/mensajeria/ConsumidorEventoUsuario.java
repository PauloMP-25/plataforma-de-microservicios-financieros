package com.suscripciones.infraestructura.mensajeria;

import com.libreria.comun.dtos.EventoLoginExitosoDTO;
import com.libreria.comun.mensajeria.NombresExchange;
import com.libreria.comun.mensajeria.RoutingKeys;
import com.suscripciones.dominio.entidades.Suscripcion;
import com.suscripciones.dominio.repositorios.SuscripcionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Consumidor para eventos de dominio de usuario, específicamente el login exitoso.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ConsumidorEventoUsuario {

    private final SuscripcionRepository suscripcionRepository;
    private final RabbitTemplate rabbitTemplate;

    /**
     * Escucha el evento de login exitoso.
     * Recibe el objeto {@link EventoLoginExitosoDTO} publicado por ms-usuario.
     * Revisa si el usuario tiene una suscripción "LUKA PREMIUM" o "LUKA PRO" vencida
     * y notifica al microservicio de usuarios para que baje su rol a FREE.
     *
     * @param evento DTO del evento de login publicado por ms-usuario.
     */
    @RabbitListener(queues = "cola.suscripciones.login.exitoso")
    public void procesarLoginUsuario(EventoLoginExitosoDTO evento) {
        log.info("Evento de login recibido para usuario: {}", evento.usuarioId());
        try {
            UUID usuarioId = evento.usuarioId();

            List<String> nombresLuka = Arrays.asList("LUKA PREMIUM", "LUKA PRO");
            List<Suscripcion> suscripciones = suscripcionRepository.findByUsuarioIdAndNombreIn(usuarioId, nombresLuka);

            for (Suscripcion sub : suscripciones) {
                if (sub.getFechaVencimiento() != null && sub.getFechaVencimiento().isBefore(LocalDate.now())) {
                    if (!"VENCIDA".equals(sub.getEstado()) && !"EXPIRADA".equals(sub.getEstado())) {
                        log.info("Suscripción {} del usuario {} ha expirado. Actualizando estado a VENCIDA.", sub.getNombre(), usuarioId);
                        sub.setEstado("VENCIDA");
                        suscripcionRepository.save(sub);

                        // Emitir evento para que ms-usuario actualice el rol
                        log.info("Emitiendo evento de suscripción expirada a exchange.suscripciones.eventos para usuario {}", usuarioId);
                        rabbitTemplate.convertAndSend("exchange.suscripciones.eventos", "suscripcion.luka.expirada", usuarioId.toString());
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error al procesar el evento de login para el usuario {}: {}", evento.usuarioId(), e.getMessage());
        }
    }
}

