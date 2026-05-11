package com.cliente.aplicacion.eventos;

import com.cliente.aplicacion.servicios.ServicioContexto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Listener transaccional que reacciona a los cambios en el contexto
 * financiero del usuario <b>después de que la transacción de BD haya
 * sido confirmada</b> (AFTER_COMMIT).
 * <p>
 * Implementa el patrón <i>Transactional Event Publisher</i>:
 * los servicios de negocio publican un {@link EventoContextoActualizado}
 * mediante {@code ApplicationEventPublisher}, y este listener lo captura
 * únicamente si el commit fue exitoso. Esto elimina el riesgo de enviar
 * mensajes a RabbitMQ/Redis sobre datos que podrían haber sido revertidos
 * por un rollback.
 * </p>
 *
 * <h3>Flujo:</h3>
 * <pre>
 * Servicio (dentro de @Transactional)
 *   └─ eventPublisher.publishEvent(new EventoContextoActualizado(...))
 *
 * BD confirma (COMMIT)
 *   └─ EscuchaSincronizacionIA.alActualizarContexto()
 *       ├─ ServicioContexto.refrescarContextoRedis(usuarioId)
 *       │   ├─ Actualiza Redis (ia:contexto:{uuid})
 *       │   └─ Publica a RabbitMQ (exchange.cliente.actualizaciones)
 *       └─ Log de trazabilidad
 * </pre>
 *
 * @author Paulo Moron
 * @version 1.1.0
 * @since 2026-05-10
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EscuchaSincronizacionIA {

    private final ServicioContexto servicioContexto;

    /**
     * Reacciona al evento de contexto actualizado después del commit.
     * <p>
     * Dispara el refresco de la caché Redis y la publicación del
     * mensaje de sincronización a RabbitMQ. Ambas operaciones son
     * asíncronas ({@code @Async} en {@code ServicioContextoImpl}),
     * por lo que no bloquean la respuesta al usuario.
     * </p>
     *
     * @param evento Evento con el ID del usuario y el origen del cambio.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void alActualizarContexto(EventoContextoActualizado evento) {
        log.info("[SYNC-LISTENER] Transacción confirmada. Sincronizando contexto IA " +
                "para usuarioId={} (origen: {})", evento.getUsuarioId(), evento.getOrigen());
        servicioContexto.refrescarContextoRedis(evento.getUsuarioId());
    }
}
