package com.mensajeria.infraestructura.mensajeria;

import com.libreria.comun.dtos.EventoAuditoriaDTO;
import com.libreria.comun.mensajeria.PublicadorEventosBase;
import java.util.UUID;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mensajeria.dominio.entidades.BandejaSalidaMensajeria;
import com.mensajeria.dominio.repositorios.RepositorioBandejaSalidaMensajeria;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

/**
 * Publicador de auditoría para mensajería.
 * Hereda de la base para aprovechar el enrutamiento estándar.
 * 
 * @author Paulo Moron
 * @version 1.2.0
 */
@Component
@Slf4j
public class PublicadorAuditoria extends PublicadorEventosBase {

    private final RepositorioBandejaSalidaMensajeria outboxRepository;
    private final ObjectMapper objectMapper;

    /**
     * Constructor con inyección de dependencias.
     */
    public PublicadorAuditoria(RabbitTemplate rabbitTemplate, 
                               RepositorioBandejaSalidaMensajeria outboxRepository,
                               ObjectMapper objectMapper) {
        super(rabbitTemplate);
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Publica un evento de seguridad de mensajería usando Patrón Outbox.
     * 
     * @param usuario id del usuario
     * @param accion  acción realizada
     * @param detalle detalle adicional
     */
    @Transactional
    public void publicarEventoSeguridad(UUID usuario, String accion, String detalle) {
        EventoAuditoriaDTO dto = EventoAuditoriaDTO.crear(
                usuario,
                accion,
                com.mensajeria.MicroservicioMensajeriaApplication.NOMBRE_SERVICIO,
                "INTERNAL",
                detalle);

        try {
            // 1. Guardar en Bandeja de Salida (dentro de la misma transacción de negocio)
            String payload = objectMapper.writeValueAsString(dto);
            BandejaSalidaMensajeria outbox = BandejaSalidaMensajeria.builder()
                    .tipoEvento(".seguridad")
                    .payload(payload)
                    .build();
            outbox = outboxRepository.save(outbox);

            // 2. Intentar enviar a RabbitMQ
            super.publicarEvento(dto, ".seguridad");

            // 3. Si tiene éxito inmediato, marcar como procesado
            outbox.setProcesado(true);
            outboxRepository.save(outbox);
            
        } catch (Exception e) {
            log.error("[OUTBOX] Error al publicar evento de auditoría inmediatamente, se reintentará luego: {}", e.getMessage());
            // No relanzar excepción, permitir que la transacción de negocio principal se complete
        }
    }
}
