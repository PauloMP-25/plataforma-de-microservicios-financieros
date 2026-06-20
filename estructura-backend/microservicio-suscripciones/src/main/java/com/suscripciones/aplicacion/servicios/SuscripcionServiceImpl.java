package com.suscripciones.aplicacion.servicios;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.libreria.comun.excepciones.ExcepcionConflicto;
import com.libreria.comun.excepciones.ExcepcionRecursoNoEncontrado;
import com.libreria.comun.utilidades.CalculadorFechasStrategy;
import com.suscripciones.aplicacion.dtos.*;
import com.suscripciones.aplicacion.puertos.ISuscripcionService;
import com.suscripciones.dominio.entidades.BandejaSalida;
import com.suscripciones.dominio.entidades.ClaveIdempotencia;
import com.suscripciones.dominio.entidades.HistorialPagoSuscripcion;
import com.suscripciones.dominio.entidades.Suscripcion;
import com.suscripciones.dominio.excepciones.*;
import com.suscripciones.dominio.repositorios.BandejaSalidaRepository;
import com.suscripciones.dominio.repositorios.ClaveIdempotenciaRepository;
import com.suscripciones.dominio.repositorios.HistorialPagoSuscripcionRepository;
import com.suscripciones.dominio.repositorios.SuscripcionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementación concreta del caso de uso de suscripciones.
 * Implementa el patrón Strategy para la asignación de fechas de cobro
 * y el patrón Transactional Outbox para la comunicación eventual resiliente.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SuscripcionServiceImpl implements ISuscripcionService {

    private final SuscripcionRepository suscripcionRepository;
    private final HistorialPagoSuscripcionRepository historialPagoSuscripcionRepository;
    private final BandejaSalidaRepository bandejaSalidaRepository;
    private final ClaveIdempotenciaRepository claveIdempotenciaRepository;
    private final List<CalculadorFechasStrategy> estrategias;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public RespuestaSuscripcion crearSuscripcion(SolicitudCrearSuscripcion solicitud) {
        log.info("Creando nueva suscripción '{}' para el usuario {}", solicitud.nombre(), solicitud.usuarioId());

        LocalDate fechaInicio = (solicitud.fechaInicio() != null) 
                ? solicitud.fechaInicio() 
                : LocalDate.now();

        String estrategiaCode = (solicitud.tipoEstrategia() != null && !solicitud.tipoEstrategia().isBlank())
                ? solicitud.tipoEstrategia().toUpperCase()
                : "CALENDARIO";

        LocalDate fechaVencimiento;
        if (solicitud.fechaVencimiento() != null) {
            fechaVencimiento = solicitud.fechaVencimiento();
        } else {
            CalculadorFechasStrategy estrategia = obtenerEstrategia(estrategiaCode);
            fechaVencimiento = estrategia.calcularSiguienteFechaPago(fechaInicio);
        }

        Suscripcion suscripcion = Suscripcion.builder()
                .usuarioId(solicitud.usuarioId())
                .nombre(solicitud.nombre())
                .monto(solicitud.monto())
                .estado("ACTIVA")
                .metodoPago(solicitud.metodoPago())
                .fechaInicio(fechaInicio)
                .fechaVencimiento(fechaVencimiento)
                .tipoEstrategia(estrategiaCode)
                .eliminado(false)
                .build();

        Suscripcion guardada = suscripcionRepository.save(suscripcion);
        return mapearARespuesta(guardada);
    }

    @Override
    @Transactional(readOnly = true)
    public RespuestaSuscripcion buscarPorId(UUID id) {
        Suscripcion suscripcion = suscripcionRepository.findById(id)
                .orElseThrow(() -> new ExcepcionSuscripcionNoEncontrada(id));
        return mapearARespuesta(suscripcion);
    }

    @Override
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<RespuestaSuscripcion> listarPorUsuario(
            UUID usuarioId, 
            String estado, 
            String metodoPago, 
            LocalDate fechaVencimientoAntes, 
            org.springframework.data.domain.Pageable pageable) {
        
        log.info("Listando suscripciones paginadas para el usuario {}", usuarioId);

        org.springframework.data.jpa.domain.Specification<Suscripcion> spec = (root, query, cb) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            
            // Filtro obligatorio: usuarioId
            predicates.add(cb.equal(root.get("usuarioId"), usuarioId));
            
            // Filtro opcional: estado
            if (estado != null && !estado.isBlank()) {
                predicates.add(cb.equal(cb.upper(root.get("estado")), estado.toUpperCase()));
            }
            
            // Filtro opcional: metodoPago
            if (metodoPago != null && !metodoPago.isBlank()) {
                predicates.add(cb.equal(cb.upper(root.get("metodoPago")), metodoPago.toUpperCase()));
            }
            
            // Filtro opcional: fechaVencimiento antes de o igual a
            if (fechaVencimientoAntes != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("fechaVencimiento"), fechaVencimientoAntes));
            }
            
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
        
        return suscripcionRepository.findAll(spec, pageable).map(this::mapearARespuesta);
    }

    @Override
    @Transactional
    public RespuestaPagoSuscripcion registrarPagoManual(UUID id, SolicitudRegistrarPagoManual solicitud, String idempotencyKey) {
        log.info("Registrando pago manual para la suscripción ID: {}", id);

        // 1. Validar Idempotencia
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            if (claveIdempotenciaRepository.existsById(idempotencyKey)) {
                log.warn("Petición duplicada detectada para clave de idempotencia: {}", idempotencyKey);
                throw new ExcepcionConflicto("idempotencyKey", idempotencyKey);
            }
            claveIdempotenciaRepository.save(new ClaveIdempotencia(idempotencyKey, LocalDateTime.now()));
        }

        // 2. Recuperar Suscripción
        Suscripcion suscripcion = suscripcionRepository.findById(id)
                .orElseThrow(() -> new ExcepcionSuscripcionNoEncontrada(id));

        LocalDate fechaPago = (solicitud.fechaPago() != null) 
                ? solicitud.fechaPago() 
                : LocalDate.now();

        BigDecimal monto = (solicitud.monto() != null) 
                ? solicitud.monto() 
                : suscripcion.getMonto();

        String metodoPago = (solicitud.metodoPago() != null && !solicitud.metodoPago().isBlank())
                ? solicitud.metodoPago()
                : suscripcion.getMetodoPago();

        // 3. Calcular Nueva Fecha de Vencimiento
        CalculadorFechasStrategy estrategia = obtenerEstrategia(suscripcion.getTipoEstrategia());
        LocalDate nuevaFechaVencimiento = estrategia.calcularSiguienteFechaPago(fechaPago);

        // 4. Actualizar Estado de la Suscripción
        suscripcion.setEstado("ACTIVA");
        suscripcion.setFechaUltimoPago(fechaPago);
        suscripcion.setFechaVencimiento(nuevaFechaVencimiento);
        suscripcionRepository.save(suscripcion);

        // 5. Registrar Histórico de Pago
        HistorialPagoSuscripcion pagoHistorial = HistorialPagoSuscripcion.builder()
                .suscripcion(suscripcion)
                .monto(monto)
                .fechaPago(fechaPago)
                .estado("EXITOSO")
                .build();
        HistorialPagoSuscripcion pagoGuardado = historialPagoSuscripcionRepository.save(pagoHistorial);

        // 6. Escribir a la Bandeja de Salida (Transactional Outbox)
        try {
            Map<String, Object> payloadMap = Map.of(
                    "historialPagoId", pagoGuardado.getId().toString(),
                    "usuarioId", suscripcion.getUsuarioId().toString(),
                    "nombre", suscripcion.getNombre(),
                    "monto", monto.toString(),
                    "metodoPago", metodoPago,
                    "fechaPago", fechaPago.toString()
            );
            String jsonPayload = objectMapper.writeValueAsString(payloadMap);
            
            BandejaSalida outbox = new BandejaSalida("EVENTO_SUSCRIPCION_PAGADA", jsonPayload);
            bandejaSalidaRepository.save(outbox);
            log.info("Evento 'EVENTO_SUSCRIPCION_PAGADA' registrado en Outbox para pago ID: {}", pagoGuardado.getId());
            
        } catch (JsonProcessingException e) {
            log.error("Error al serializar el evento de outbox para pago ID: {}", pagoGuardado.getId(), e);
            throw new RuntimeException("Error interno de procesamiento de eventos", e);
        }

        return new RespuestaPagoSuscripcion(
                pagoGuardado.getId(),
                suscripcion.getId(),
                pagoGuardado.getTransaccionId(),
                monto.setScale(2, RoundingMode.HALF_UP),
                fechaPago,
                pagoGuardado.getEstado()
        );
    }

    @Override
    @Transactional
    public RespuestaSuscripcion cancelarSuscripcion(UUID id) {
        log.info("Cancelando suscripción ID: {}", id);

        Suscripcion suscripcion = suscripcionRepository.findById(id)
                .orElseThrow(() -> new ExcepcionSuscripcionNoEncontrada(id));

        if ("CANCELADA".equals(suscripcion.getEstado())) {
            log.warn("Intento de cancelar una suscripción que ya está CANCELADA: {}", id);
            throw new ExcepcionSuscripcionYaCancelada(id);
        }

        suscripcion.setEstado("CANCELADA");
        Suscripcion guardada = suscripcionRepository.save(suscripcion);

        // Enviar evento de cancelación a la bandeja de salida (Outbox) para notificaciones
        try {
            Map<String, Object> payloadMap = Map.of(
                    "suscripcionId", guardada.getId().toString(),
                    "usuarioId", guardada.getUsuarioId().toString(),
                    "nombre", guardada.getNombre(),
                    "estado", guardada.getEstado()
            );
            String jsonPayload = objectMapper.writeValueAsString(payloadMap);
            
            BandejaSalida outbox = new BandejaSalida("EVENTO_SUSCRIPCION_CANCELADA", jsonPayload);
            bandejaSalidaRepository.save(outbox);
            log.info("Evento 'EVENTO_SUSCRIPCION_CANCELADA' registrado en Outbox para suscripción ID: {}", guardada.getId());
        } catch (JsonProcessingException e) {
            log.error("Error al serializar evento de cancelación en Outbox para suscripción ID: {}", guardada.getId(), e);
        }

        return mapearARespuesta(guardada);
    }

    @Override
    @Transactional
    public RespuestaSuscripcion editarSuscripcion(UUID id, SolicitudEditarSuscripcion solicitud) {
        log.info("Editando suscripción ID: {}", id);

        Suscripcion suscripcion = suscripcionRepository.findById(id)
                .orElseThrow(() -> new ExcepcionSuscripcionNoEncontrada(id));

        if (solicitud.monto() != null) {
            suscripcion.setMonto(solicitud.monto());
        }
        if (solicitud.metodoPago() != null && !solicitud.metodoPago().isBlank()) {
            suscripcion.setMetodoPago(solicitud.metodoPago());
        }
        if (solicitud.tipoEstrategia() != null && !solicitud.tipoEstrategia().isBlank()) {
            String estrategiaCode = solicitud.tipoEstrategia().toUpperCase();
            CalculadorFechasStrategy estrategia = obtenerEstrategia(estrategiaCode);
            suscripcion.setTipoEstrategia(estrategiaCode);

            // Recalcula la fecha de vencimiento a partir de la fecha del último pago (o inicio si no hay pago)
            LocalDate fechaReferencia = (suscripcion.getFechaUltimoPago() != null) 
                    ? suscripcion.getFechaUltimoPago() 
                    : suscripcion.getFechaInicio();
            suscripcion.setFechaVencimiento(estrategia.calcularSiguienteFechaPago(fechaReferencia));
        }

        Suscripcion guardada = suscripcionRepository.save(suscripcion);
        return mapearARespuesta(guardada);
    }

    private CalculadorFechasStrategy obtenerEstrategia(String tipoEstrategia) {
        return estrategias.stream()
                .filter(e -> e.soporta(tipoEstrategia))
                .findFirst()
                .orElseThrow(() -> new ExcepcionEstrategiaNoSoportada(tipoEstrategia));
    }

    private RespuestaSuscripcion mapearARespuesta(Suscripcion suscripcion) {
        return new RespuestaSuscripcion(
                suscripcion.getId(),
                suscripcion.getNombre(),
                suscripcion.getMonto().setScale(2, RoundingMode.HALF_UP),
                suscripcion.getEstado(),
                suscripcion.getMetodoPago(),
                suscripcion.getFechaInicio(),
                suscripcion.getFechaVencimiento(),
                suscripcion.getFechaUltimoPago(),
                suscripcion.getTipoEstrategia()
        );
    }
}
