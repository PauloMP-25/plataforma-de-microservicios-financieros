package com.suscripciones.infraestructura.tareas;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.libreria.comun.dtos.EventoSuscripcionPagadaDTO;
import com.libreria.comun.mensajeria.NombresExchange;
import com.libreria.comun.mensajeria.RoutingKeys;
import com.libreria.comun.respuesta.ResultadoApi;
import com.libreria.comun.seguridad.DetallesUsuario;
import com.libreria.comun.seguridad.ServicioJwt;
import com.suscripciones.dominio.entidades.BandejaSalida;
import com.suscripciones.dominio.repositorios.BandejaSalidaRepository;
import com.suscripciones.dominio.repositorios.HistorialPagoSuscripcionRepository;
import com.suscripciones.infraestructura.clientes.NucleoFinancieroClient;
import com.suscripciones.infraestructura.clientes.dtos.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.MDC;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Tarea programada (Scheduler) encargada de procesar los eventos de la Bandeja de Salida (Outbox).
 * Garantiza consistencia eventual con ShedLock para entornos multi-réplica.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxScheduler {

    private final BandejaSalidaRepository bandejaSalidaRepository;
    private final HistorialPagoSuscripcionRepository historialPagoSuscripcionRepository;
    private final NucleoFinancieroClient nucleoFinancieroClient;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;
    private final ServicioJwt servicioJwt;

    @Scheduled(fixedDelayString = "${luka.scheduler.outbox.delay:5000}")
    @SchedulerLock(name = "procesarOutbox", lockAtMostFor = "10s", lockAtLeastFor = "2s")
    @Transactional
    public void procesarOutbox() {
        List<BandejaSalida> eventos = bandejaSalidaRepository.findByProcesadoFalseOrderByFechaCreacionAsc();
        if (eventos.isEmpty()) {
            return;
        }

        log.info("Procesando {} eventos de outbox pendientes...", eventos.size());

        for (BandejaSalida evento : eventos) {
            try {
                if ("EVENTO_SUSCRIPCION_PAGADA".equals(evento.getTipoEvento())) {
                    procesarPagoSuscripcion(evento);
                } else if ("EVENTO_SUSCRIPCION_CANCELADA".equals(evento.getTipoEvento())) {
                    procesarCancelacionSuscripcion(evento);
                } else if ("EVENTO_SUSCRIPCION_VENCIDA".equals(evento.getTipoEvento())) {
                    procesarVencimientoSuscripcion(evento);
                } else {
                    log.warn("Tipo de evento desconocido en outbox: {}", evento.getTipoEvento());
                    evento.setProcesado(true);
                    evento.setFechaProceso(LocalDateTime.now());
                    bandejaSalidaRepository.save(evento);
                }
            } catch (com.suscripciones.dominio.excepciones.ExcepcionLogicaOutbox e) {
                log.error("Fallo lógico irrecuperable en evento outbox ID: {} (tipo {}). No se reintentará. Error: {}", 
                        evento.getId(), evento.getTipoEvento(), e.getMessage());
                evento.setProcesado(true);
                evento.setIntentos(5); // Marcar como agotado
                evento.setFechaProceso(LocalDateTime.now());
                bandejaSalidaRepository.save(evento);
            } catch (Exception e) {
                log.error("Error al procesar evento de outbox ID: {}. Intento: {}", 
                        evento.getId(), evento.getIntentos(), e);
                evento.setIntentos(evento.getIntentos() + 1);
                if (evento.getIntentos() >= 5) {
                    evento.setProcesado(true); // Se marca como procesado para no bloquear la cola de outbox
                    log.error("Evento ID: {} ha excedido el número de intentos máximos (5). Se detiene el reintento.", evento.getId());
                }
                bandejaSalidaRepository.save(evento);
            }
        }
    }

    private void procesarPagoSuscripcion(BandejaSalida evento) throws JsonProcessingException {
        JsonNode payload = objectMapper.readTree(evento.getPayload());
        
        UUID historialPagoId = UUID.fromString(payload.get("historialPagoId").asText());
        UUID usuarioId = UUID.fromString(payload.get("usuarioId").asText());
        String nombre = payload.get("nombre").asText();
        BigDecimal monto = new BigDecimal(payload.get("monto").asText());
        String metodoPagoStr = payload.get("metodoPago").asText();
        LocalDate fechaPago = LocalDate.parse(payload.get("fechaPago").asText());

        // Generar un token JWT efímero para este usuario con el rol adecuado para autorizar la llamada inter-servicio
        DetallesUsuario detallesUsuario = DetallesUsuario.builder()
                .usuarioId(usuarioId)
                .email("sistema-suscripciones@luka.com")
                .roles(List.of("ROLE_PREMIUM"))
                .build();
        String token = "Bearer " + servicioJwt.generarToken(detallesUsuario, Map.of("usuarioId", usuarioId.toString()));

        // 1. Obtener Categoria ID del núcleo financiero
        UUID categoriaId = obtenerCategoriaId(token);
        if (categoriaId == null) {
            log.error("No se pudo obtener el ID de categoría del núcleo financiero para el evento pago ID: {}. Abortando intento.", historialPagoId);
            throw new IllegalStateException("Categoría no disponible en núcleo financiero");
        }

        // Obtener frecuencia de la suscripción para el concepto de la transacción
        var pagoOpt = historialPagoSuscripcionRepository.findById(historialPagoId);
        String tipoEstrategia = pagoOpt.isPresent() ? pagoOpt.get().getSuscripcion().getTipoEstrategia() : "CALENDARIO";
        String frecuencia = "mensual";
        if (tipoEstrategia != null) {
            String upper = tipoEstrategia.toUpperCase();
            if (upper.contains("SEMANAL")) {
                frecuencia = "semanal";
            } else if (upper.contains("DIARIO")) {
                frecuencia = "diario";
            } else if (upper.contains("ANUAL")) {
                frecuencia = "anual";
            }
        }

        // 2. Registrar transacción (gasto) en núcleo financiero
        SolicitudTransaccion solicitud = new SolicitudTransaccion(
                usuarioId,
                "Pago " + frecuencia + " al servicio " + nombre,
                monto,
                TipoMovimiento.GASTO,
                categoriaId,
                mapearMetodoPago(metodoPagoStr),
                "suscripcion,sistema",
                "Pago " + frecuencia + " de suscripción " + nombre
        );

        log.info("Enviando registro de transacción al núcleo financiero para el pago ID: {}", historialPagoId);
        ResultadoApi<RespuestaTransaccion> resp = nucleoFinancieroClient.registrarTransaccion(token, solicitud);

        if (resp != null && resp.exito() && resp.datos() != null) {
            log.info("Transacción registrada con éxito en núcleo financiero. ID Retornado: {}", resp.datos().id());
            
            // 3. Vincular transaccionId en el historial local
            historialPagoSuscripcionRepository.findById(historialPagoId).ifPresent(h -> {
                h.setTransaccionId(resp.datos().id());
                historialPagoSuscripcionRepository.save(h);
            });

            // 4. Marcar evento outbox como procesado
            evento.setProcesado(true);
            evento.setFechaProceso(LocalDateTime.now());
            bandejaSalidaRepository.save(evento);

            // 5. Despachar mensaje de confirmación a RabbitMQ para notificaciones (ms-mensajeria)
            despacharNotificacionRabbitMQ(resp.datos().id(), usuarioId, nombre, monto, fechaPago);
            
        } else {
            String errorMsg = (resp != null) ? resp.mensaje() : "Respuesta nula del cliente Feign";
            int estado = (resp != null) ? resp.estado() : 500;
            log.error("El núcleo financiero devolvió error (estado {}): {}", estado, errorMsg);
            
            if (estado >= 400 && estado < 500 && estado != 429) {
                throw new com.suscripciones.dominio.excepciones.ExcepcionLogicaOutbox(
                        "Fallo definitivo 4xx en registro de transacción remota: " + errorMsg);
            } else {
                throw new RuntimeException("Fallo temporal en registro de transacción remota: " + errorMsg);
            }
        }
    }

    private void procesarCancelacionSuscripcion(BandejaSalida evento) throws JsonProcessingException {
        JsonNode payload = objectMapper.readTree(evento.getPayload());
        UUID usuarioId = UUID.fromString(payload.get("usuarioId").asText());
        String nombre = payload.get("nombre").asText();

        log.info("Procesando evento de cancelación para el usuario {} y suscripción {}", usuarioId, nombre);

        evento.setProcesado(true);
        evento.setFechaProceso(LocalDateTime.now());
        bandejaSalidaRepository.save(evento);
    }

    private void procesarVencimientoSuscripcion(BandejaSalida evento) throws JsonProcessingException {
        JsonNode payload = objectMapper.readTree(evento.getPayload());
        UUID usuarioId = UUID.fromString(payload.get("usuarioId").asText());
        String nombre = payload.get("nombre").asText();

        log.info("Procesando evento de vencimiento para el usuario {} y suscripción {}", usuarioId, nombre);

        evento.setProcesado(true);
        evento.setFechaProceso(LocalDateTime.now());
        bandejaSalidaRepository.save(evento);
    }

    private UUID obtenerCategoriaId(String token) {
        try {
            ResultadoApi<List<CategoriaDTO>> respuesta = nucleoFinancieroClient.listarCategorias(token, TipoMovimiento.GASTO);
            if (respuesta != null && respuesta.exito() && respuesta.datos() != null) {
                // 1. Intentar buscar "Suscripciones Streaming" o "Suscripciones"
                for (CategoriaDTO cat : respuesta.datos()) {
                    if (cat.nombre().toLowerCase().contains("suscripción") || cat.nombre().toLowerCase().contains("suscripcion")) {
                        return cat.id();
                    }
                }
                // 2. Fallback a "Otros Gastos"
                for (CategoriaDTO cat : respuesta.datos()) {
                    if (cat.nombre().toLowerCase().contains("otros")) {
                        return cat.id();
                    }
                }
                // 3. Fallback al primer elemento disponible
                if (!respuesta.datos().isEmpty()) {
                    return respuesta.datos().get(0).id();
                }
            }
        } catch (Exception e) {
            log.error("Excepción al listar categorías desde núcleo financiero: ", e);
        }
        return null;
    }

    private MetodoPago mapearMetodoPago(String metodo) {
        if (metodo == null) return MetodoPago.DIGITAL;
        try {
            return MetodoPago.valueOf(metodo.toUpperCase());
        } catch (IllegalArgumentException e) {
            String upper = metodo.toUpperCase();
            if (upper.contains("STRIPE") || upper.contains("PAYPAL") || upper.contains("DIGITAL")) {
                return MetodoPago.DIGITAL;
            }
            if (upper.contains("TARJETA") || upper.contains("CREDITO") || upper.contains("DEBITO")) {
                return MetodoPago.TARJETA;
            }
            if (upper.contains("TRANSFERENCIA") || upper.contains("BANCO")) {
                return MetodoPago.TRANSFERENCIA;
            }
            return MetodoPago.EFECTIVO;
        }
    }

    private void despacharNotificacionRabbitMQ(UUID transaccionId, UUID usuarioId, String nombre, BigDecimal monto, LocalDate fechaPago) {
        try {
            String corrId = MDC.get("correlationId");
            if (corrId == null) {
                corrId = UUID.randomUUID().toString();
            }

            EventoSuscripcionPagadaDTO dto = new EventoSuscripcionPagadaDTO(
                    transaccionId,
                    usuarioId,
                    nombre,
                    monto,
                    fechaPago.plusMonths(1).atStartOfDay(), // fechaVencimiento aproximada
                    fechaPago.atStartOfDay(),
                    corrId
            );

            log.info("Publicando EventoSuscripcionPagadaDTO a RabbitMQ. RoutingKey: {}", RoutingKeys.PAGO_EXITOSO);
            rabbitTemplate.convertAndSend(NombresExchange.PAGOS, RoutingKeys.PAGO_EXITOSO, dto);
        } catch (Exception e) {
            log.error("Fallo al publicar la notificación a RabbitMQ para el usuario {}", usuarioId, e);
        }
    }
}
