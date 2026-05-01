package com.nucleo.financiero.infraestructura.mensajeria;

import com.nucleo.financiero.aplicacion.dtos.auditoria.AuditoriaAccesoRequestDTO;
import com.nucleo.financiero.aplicacion.dtos.auditoria.AuditoriaTransaccionalRequestDTO;
import com.nucleo.financiero.aplicacion.dtos.auditoria.EstadoAcceso;
import com.nucleo.financiero.aplicacion.dtos.auditoria.RegistroAuditoriaDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class PublicadorAuditoria {

    private final RabbitTemplate rabbitTemplate;

    // Nombres de exchange y routing keys definidos en tu config de Rabbit
    private static final String EXCHANGE = "exchange.auditoria";
    private static final String RK_TRANSACCION = "auditoria.transaccion.registro";
    private static final String RK_ACCESO = "auditoria.acceso.lectura";
    public static final String RK_IA = "ia.analisis.routing";

    public void publicarRegistro(UUID usuarioId, String entidadId, String valorNuevo, String ip) {
        AuditoriaTransaccionalRequestDTO dto = new AuditoriaTransaccionalRequestDTO(
                usuarioId,
                "MICROSERVICIO-NUCLEO-FINANCIERO",
                "TRANSACCION",
                entidadId,
                null, // valorAnterior es null en creaciones
                valorNuevo,
                LocalDateTime.now()
        );

        rabbitTemplate.convertAndSend(EXCHANGE, RK_TRANSACCION, dto);
    }

    // Para CONSULTAR datos (Lectura/Acceso)
    public void publicarAcceso(UUID usuarioId, String accion, String mensaje, String ip) {
        // Usamos el DTO de acceso que espera tu consumidor de auditoría
        AuditoriaAccesoRequestDTO dto = new AuditoriaAccesoRequestDTO(
                usuarioId,
                ip,
                "MODULO-NUCLEO-FINANCIERO",
                EstadoAcceso.EXITO,
                mensaje,
                LocalDateTime.now()
        );
        rabbitTemplate.convertAndSend(EXCHANGE, RK_ACCESO, dto);
    }

    public void enviarEventoAuditoria(RegistroAuditoriaDTO evento) {
        rabbitTemplate.convertAndSend(
                EXCHANGE,
                RK_IA,
                evento
        );
    }
}
