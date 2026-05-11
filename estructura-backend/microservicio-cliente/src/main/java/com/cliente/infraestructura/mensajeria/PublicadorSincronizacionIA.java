package com.cliente.infraestructura.mensajeria;

import com.libreria.comun.dtos.ContextoEstrategicoIADTO;
import com.libreria.comun.mensajeria.PublicadorEventosBase;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Publicador especializado para la sincronización en tiempo real del
 * contexto financiero del cliente hacia el microservicio-ia.
 * <p>
 * Extiende de {@link PublicadorEventosBase} y encapsula la lógica de envío
 * del {@link ContextoEstrategicoIADTO} completo a través de RabbitMQ.
 * El mensaje se publica en {@code exchange.cliente.actualizaciones} con
 * routing key {@code cliente.perfil.actualizado}, y es consumido por el
 * {@code EscuchadorSincronizacionIA} en ms-ia, quien actualiza la caché
 * Redis {@code ia:contexto:{usuarioId}} sin necesidad de consultar la DB.
 * </p>
 *
 * <h3>Flujo completo:</h3>
 * <pre>
 * ms-cliente (escritura) → PublicadorSincronizacionIA → RabbitMQ
 *     → cola.ia.sincronizacion.contexto → ms-ia (Python)
 *     → Redis (ia:contexto:{usuarioId})
 * </pre>
 *
 * @author Paulo Moron
 * @version 1.1.0
 * @since 2026-05-10
 */
@Slf4j
@Component
public class PublicadorSincronizacionIA extends PublicadorEventosBase {

    /**
     * Constructor que inyecta el RabbitTemplate a la clase base.
     *
     * @param rabbitTemplate Cliente de RabbitMQ configurado por Spring.
     */
    public PublicadorSincronizacionIA(RabbitTemplate rabbitTemplate) {
        super(rabbitTemplate);
    }

    /**
     * Publica el contexto estratégico actualizado del cliente de forma asíncrona.
     * <p>
     * Este método debe invocarse tras cualquier operación de escritura
     * (crear, actualizar, eliminar) en los servicios de perfil, metas o límites.
     * El mensaje contiene el DTO completo para que el consumidor no tenga que
     * realizar consultas adicionales a la base de datos. El {@code usuarioId}
     * se inyecta como header AMQP para que el consumidor identifique la clave Redis.
     * </p>
     *
     * @param usuarioId ID del usuario propietario del contexto.
     * @param contexto  DTO ligero con el contexto financiero optimizado para IA.
     */
    @Async
    public void publicarActualizacionContexto(UUID usuarioId, ContextoEstrategicoIADTO contexto) {
        log.info("[SYNC-IA] Publicando contexto actualizado para usuarioId={}, nombres='{}'",
                usuarioId, contexto.nombres());
        this.publicarSincronizacionCliente(contexto, usuarioId.toString());
    }
}
