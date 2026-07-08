package com.cliente.infraestructura.mensajeria;

import com.cliente.aplicacion.puertos.ServicioContexto;
import com.cliente.aplicacion.puertos.ServicioLimiteGasto;
import com.libreria.comun.dtos.EventoLoginExitosoDTO;
import com.libreria.comun.mensajeria.NombresCola;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Consumidor de eventos de login exitoso provenientes del microservicio de usuario.
 * <p>
 * Escucha la cola {@code cola.cliente.login.eventos} y ejecuta las tareas
 * de inicialización post-login para el usuario:
 * <ol>
 *   <li>Verifica y desactiva automáticamente el límite de gasto si ha vencido.</li>
 *   <li>Refresca el caché Redis con el contexto financiero del cliente.</li>
 * </ol>
 * La lógica es idempotente: si no hay límite activo o si el límite no venció,
 * retorna sin realizar cambios. Si una tarea falla, se loguea el error y
 * se continúa con las demás para maximizar la resiliencia.
 * </p>
 *
 * @see ServicioLimiteGasto#verificarYDesactivarSiVencido(UUID)
 * @see ServicioContexto#refrescarContextoRedis(UUID)
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ConsumidorLoginExitoso {

    private final ServicioLimiteGasto servicioLimiteGasto;
    private final ServicioContexto servicioContexto;

    /**
     * Procesa el evento de login exitoso para un usuario.
     * <p>
     * Ejecuta las tareas post-login de forma secuencial. Si una tarea falla,
     * se loguea el error y se continúa con las demás para maximizar la resiliencia.
     * En caso de error crítico al deserializar el evento, el mensaje es relanzado
     * para que RabbitMQ lo redirija a la DLQ.
     * </p>
     *
     * @param evento Evento de login exitoso deserializado automáticamente por Spring-AMQP.
     */
    @RabbitListener(queues = NombresCola.CLIENTE_LOGIN_EVENTOS)
    public void procesarLoginExitoso(EventoLoginExitosoDTO evento) {
        UUID usuarioId = evento.usuarioId();
        String correlationId = evento.correlationId() != null ? evento.correlationId() : "N/A";

        log.info("[LOGIN-EXITOSO] Procesando evento post-login para usuario: {} [correlationId: {}]",
                usuarioId, correlationId);

        // Tarea 1: Verificar y desactivar límite de gasto vencido (idempotente)
        try {
            servicioLimiteGasto.verificarYDesactivarSiVencido(usuarioId);
            log.debug("[LOGIN-EXITOSO] Verificación de límite completada para usuario: {}", usuarioId);
        } catch (Exception e) {
            log.error("[LOGIN-EXITOSO] Error al verificar límite para usuario: {}. Causa: {}",
                    usuarioId, e.getMessage());
            // Continúa con las demás tareas — no bloquea el flujo
        }

        // Tarea 2: Refrescar el caché Redis con el contexto financiero actualizado
        try {
            servicioContexto.refrescarContextoRedis(usuarioId);
            log.debug("[LOGIN-EXITOSO] Contexto Redis refrescado para usuario: {}", usuarioId);
        } catch (Exception e) {
            log.error("[LOGIN-EXITOSO] Error al refrescar contexto Redis para usuario: {}. Causa: {}",
                    usuarioId, e.getMessage());
        }

        log.info("[LOGIN-EXITOSO] Evento post-login completado para usuario: {} [correlationId: {}]",
                usuarioId, correlationId);
    }
}
