package com.suscripciones.infraestructura.mensajeria;

import com.libreria.comun.dtos.EventoPagoExitosoDTO;
import com.suscripciones.dominio.entidades.HistorialPagoSuscripcion;
import com.suscripciones.dominio.entidades.Suscripcion;
import com.suscripciones.dominio.repositorios.HistorialPagoSuscripcionRepository;
import com.suscripciones.dominio.repositorios.SuscripcionRepository;
import com.suscripciones.infraestructura.configuracion.ConfiguracionRabbitMQ;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Consumidor de eventos de RabbitMQ para el microservicio de suscripciones.
 * Escucha las confirmaciones de pago exitosas del plan SaaS (Stripe) para
 * crear o actualizar la suscripción Luka App del usuario y su historial de forma síncrona.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ConsumidorEventoPago {

    private final SuscripcionRepository suscripcionRepository;
    private final HistorialPagoSuscripcionRepository historialPagoSuscripcionRepository;

    @RabbitListener(queues = ConfiguracionRabbitMQ.COLA_SUSCRIPCIONES_PAGOS)
    @Transactional
    public void procesarPagoPlanSaaS(EventoPagoExitosoDTO evento) {
        // Detectar dinámicamente la pasarela: Stripe usa prefijo "cs_"; MP usa preapproval_id
        String metodoPago = (evento.referenciaPasarela() != null
                && evento.referenciaPasarela().startsWith("cs_"))
                ? "STRIPE"
                : "MERCADOPAGO";

        log.info("[SUSCRIPCIONES-CONSUMER] Recibido pago exitoso via {} para usuario: {} - Plan: {}",
                metodoPago, evento.usuarioId(), evento.planNuevo());

        try {
            String planCapitalizado = evento.planNuevo().substring(0, 1).toUpperCase() + evento.planNuevo().substring(1).toLowerCase();
            String nombreSuscripcion = "Luka " + planCapitalizado;
            
            // 1. Buscar si el usuario ya tiene una suscripción activa para la plataforma Luka App
            List<Suscripcion> suscripciones = suscripcionRepository.findByUsuarioId(evento.usuarioId());
            Optional<Suscripcion> suscripcionOpt = suscripciones.stream()
                    .filter(s -> s.getNombre().startsWith("Luka"))
                    .findFirst();

            Suscripcion suscripcion;
            if (suscripcionOpt.isPresent()) {
                suscripcion = suscripcionOpt.get();
                suscripcion.setNombre(nombreSuscripcion);
                suscripcion.setMonto(evento.monto());
                suscripcion.setEstado("ACTIVA");
                suscripcion.setMetodoPago(metodoPago);
                suscripcion.setFechaInicio(evento.fechaInicioPlan().toLocalDate());
                suscripcion.setFechaVencimiento(evento.fechaFinPlan().toLocalDate());
                suscripcion.setFechaUltimoPago(evento.fechaInicioPlan().toLocalDate());
            } else {
                suscripcion = Suscripcion.builder()
                        .usuarioId(evento.usuarioId())
                        .nombre(nombreSuscripcion)
                        .monto(evento.monto())
                        .estado("ACTIVA")
                        .metodoPago(metodoPago)
                        .fechaInicio(evento.fechaInicioPlan().toLocalDate())
                        .fechaVencimiento(evento.fechaFinPlan().toLocalDate())
                        .fechaUltimoPago(evento.fechaInicioPlan().toLocalDate())
                        .tipoEstrategia("CALENDARIO")
                        .eliminado(false)
                        .build();
            }

            Suscripcion guardada = suscripcionRepository.save(suscripcion);
            log.info("[SUSCRIPCIONES-SUCCESS] Suscripción '{}' guardada/actualizada para usuario: {}", 
                    nombreSuscripcion, evento.usuarioId());

            // 2. Registrar en el histórico de pagos local
            // Nota: No se genera evento outbox de transacción para el núcleo financiero,
            // ya que ms-nucleo-financiero registra este ingreso Stripe de forma autónoma.
            HistorialPagoSuscripcion pagoHistorial = HistorialPagoSuscripcion.builder()
                    .suscripcion(guardada)
                    .monto(evento.monto())
                    .fechaPago(evento.fechaInicioPlan().toLocalDate())
                    .estado("EXITOSO")
                    .build();
            
            historialPagoSuscripcionRepository.save(pagoHistorial);
            log.info("[SUSCRIPCIONES-SUCCESS] Historial de pago guardado para suscripción ID: {}", guardada.getId());

        } catch (Exception e) {
            log.error("[SUSCRIPCIONES-ERROR] Error al procesar evento de pago SaaS para usuario {}: {}", 
                    evento.usuarioId(), e.getMessage(), e);
            throw e; // Se relanza para reintento/DLQ en RabbitMQ
        }
    }
}
