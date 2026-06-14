package com.pagos.infraestructura.mensajeria;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.libreria.comun.dtos.EventoPagoExitosoDTO;
import com.libreria.comun.mensajeria.NombresExchange;
import com.libreria.comun.mensajeria.RoutingKeys;
import com.pagos.aplicacion.puertos.IPublicadorPagos;
import com.pagos.dominio.entidades.BandejaSalida;
import com.pagos.dominio.entidades.Pago;
import com.pagos.dominio.repositorios.RepositorioBandejaSalida;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Publicador de eventos de pago con implementación del Patrón Outbox.
 * Garantiza la entrega de mensajes persistiendo el evento antes de enviarlo.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PublicadorPagosImpl implements IPublicadorPagos {

    private final RabbitTemplate rabbitTemplate;
    private final RepositorioBandejaSalida repositorioBandejaSalida;
    private final ObjectMapper objectMapper;

    @SuppressWarnings("null")
    @Override
    @Transactional
    public void publicarPagoExitoso(Pago pago, String emailUsuario) {
        log.info("[OUTBOX] Preparando evento de pago exitoso para usuario: {} con email: {}", pago.getUsuarioId(), emailUsuario);

        EventoPagoExitosoDTO evento = new EventoPagoExitosoDTO(
            pago.getId(),
            pago.getUsuarioId(),
            emailUsuario,
            pago.getDetalles().get(0).getPlanSolicitado().name(),
            pago.getDetalles().get(0).getMonto(),
            pago.getDetalles().get(0).getMoneda(),
            pago.getFechaInicioPlan(),
            pago.getFechaFinPlan(),
            pago.getStripeSessionId()
        );

        // Paso 1: Persistir en la Bandeja de Salida (Misma transacción que el negocio)
        BandejaSalida entrada = null;
        try {
            entrada = BandejaSalida.builder()
                .tipoEvento(RoutingKeys.PAGO_EXITOSO)
                .payload(objectMapper.writeValueAsString(evento))
                .procesado(false)
                .intentos(0)
                .build();
            entrada = repositorioBandejaSalida.save(entrada);
        } catch (Exception e) {
            log.error("[OUTBOX-ERROR] No se pudo guardar en bandeja de salida: {}", e.getMessage());
            throw new RuntimeException("Fallo crítico en patrón Outbox", e);
        }

        // Paso 2: Publicar en RabbitMQ (Si tiene éxito, se marca como procesado de inmediato)
        try {
            rabbitTemplate.convertAndSend(NombresExchange.PAGOS, RoutingKeys.PAGO_EXITOSO, evento);
            log.info("[RABBITMQ] Evento enviado exitosamente: {}", RoutingKeys.PAGO_EXITOSO);
            
            // Marcar de inmediato como procesado para evitar la latencia de 60 segundos del scheduler
            entrada.setProcesado(true);
            entrada.setFechaProceso(java.time.LocalDateTime.now());
            repositorioBandejaSalida.save(entrada);
            log.info("[OUTBOX-IMMEDIATE] Evento de pago exitoso marcado como procesado localmente en outbox.");
        } catch (Exception e) {
            log.warn("[RABBITMQ-WARN] Fallo al enviar a RabbitMQ de forma inmediata. El reintentador se encargará. Error: {}", e.getMessage());
        }
    }

    @Override
    @Transactional
    public void publicarPagoFallido(Pago pago) {
        log.warn("[PAGOS] Publicación de pago fallido no implementada con Outbox aún.");
        // Podríamos implementar lógica similar para PAGO_FALLIDO si fuera crítico
    }
}
