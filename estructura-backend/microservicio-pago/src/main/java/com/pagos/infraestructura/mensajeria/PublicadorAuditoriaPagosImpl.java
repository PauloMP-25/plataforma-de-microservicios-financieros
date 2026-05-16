package com.pagos.infraestructura.mensajeria;

import com.libreria.comun.dtos.EventoAccesoDTO;
import com.libreria.comun.dtos.EventoAuditoriaDTO;
import com.libreria.comun.dtos.EventoTransaccionalDTO;
import com.libreria.comun.enums.EstadoEvento;
import com.libreria.comun.mensajeria.PublicadorEventosBase;
import com.libreria.comun.utilidades.UtilidadIp;
import com.libreria.comun.utilidades.UtilidadSeguridad;
import com.pagos.dominio.entidades.Pago;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Publicador especializado en auditoría para el módulo de pagos.
 * Centraliza el envío de trazas de seguridad y transacciones hacia
 * ms-auditoria de forma explícita.
 */
@Slf4j
@Component
public class PublicadorAuditoriaPagosImpl extends PublicadorEventosBase {

    public PublicadorAuditoriaPagosImpl(RabbitTemplate rabbitTemplate) {
        super(rabbitTemplate);
    }

    /**
     * Registra el ciclo de vida de un pago.
     * Se usa EventoTransaccionalDTO porque el pago es la entidad financiera principal.
     */
    @SuppressWarnings("null")
    public void auditarCambioEstadoPago(Pago pago, String estadoAnterior) {
        EventoTransaccionalDTO dto = EventoTransaccionalDTO.crear(
                pago.getUsuarioId() != null ? pago.getUsuarioId() : UtilidadSeguridad.obtenerUsuarioId(),
                pago.getId(),
                "microservicio-pago",
                "PAGO",
                "Cambio de estado en pago Stripe",
                estadoAnterior,
                pago.getEstado().name());
        
        super.publicarTransaccion(dto, "pago");
    }

    /**
     * Registra eventos críticos de seguridad o integración.
     * Se usa EventoAuditoriaDTO para eventos que no son cambios de estado de
     * entidad.
     */
    public void auditarEventoSeguridad(UUID usuarioId, String accion, String detalle, EstadoEvento estado, HttpServletRequest request) {
        EventoAuditoriaDTO dto = EventoAuditoriaDTO.crear(
                usuarioId != null ? usuarioId : UtilidadSeguridad.obtenerUsuarioId(),
                accion,
                "MICROSERVICIO-PAGOS",
                UtilidadIp.obtenerIpReal(request),
                detalle + " | Estado: " + estado.name());

        super.publicarEvento(dto, accion);
    }

    /**
     * Registra el acceso a datos financieros sensibles por parte de administradores.
     */
    public void auditarAccesoAdmin(UUID adminId, String endpoint, String ip) {
        // En los Records, pasamos los argumentos directamente al método factory .de()
        EventoAccesoDTO dto = EventoAccesoDTO.de(
                adminId != null ? adminId : UtilidadSeguridad.obtenerUsuarioId(),
                ip != null ? ip : "127.0.0.1",
                EstadoEvento.EXITO,
                "Acceso al endpoint: " + endpoint,
                adminId != null ? adminId.toString() : UUID.randomUUID().toString()
        );

        super.publicarAcceso(dto, EstadoEvento.EXITO);
    }
}
