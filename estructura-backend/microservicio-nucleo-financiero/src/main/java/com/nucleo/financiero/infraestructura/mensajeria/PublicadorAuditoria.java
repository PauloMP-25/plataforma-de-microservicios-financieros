package com.nucleo.financiero.infraestructura.mensajeria;

import com.libreria.comun.dtos.EventoAccesoDTO;
import com.libreria.comun.dtos.EventoTransaccionalDTO;
import com.libreria.comun.enums.EstadoEvento;
import com.libreria.comun.mensajeria.PublicadorEventosBase;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Publicador de eventos de auditoría para el Núcleo Financiero.
 * <p>
 * Extiende de {@link PublicadorEventosBase} para heredar la lógica de envío
 * asíncrona centralizada.
 * Utiliza los DTOs oficiales de la librería común para asegurar la
 * compatibilidad con el ms-auditoria.
 * </p>
 *
 * @author Luka-Dev-Backend
 * @version 1.2.1
 */
@Component
public class PublicadorAuditoria extends PublicadorEventosBase {

        public PublicadorAuditoria(RabbitTemplate rabbitTemplate) {
                super(rabbitTemplate);
        }

        /**
         * Publica un registro de auditoría transaccional para trazabilidad de cambios.
         * 
         * @param usuarioId  ID del usuario que realiza la acción.
         * @param entidadId  ID de la entidad afectada (Transacción, Categoría).
         * @param valorNuevo Representación JSON o String del nuevo estado del objeto.
         * @param ip         Dirección IP desde donde se realiza la acción.
         */
        public void publicarRegistro(UUID usuarioId, String entidadId, String valorNuevo, String ip) {
                // Mapeo al DTO oficial de la librería
                                EventoTransaccionalDTO dto = EventoTransaccionalDTO.crear(
                                usuarioId,
                                UUID.fromString(entidadId),
                                                "MICROSERVICIO-NUCLEO-FINANCIERO",
                                                "TRANSACCION",
                                "REGISTRO_MOVIMIENTO",
                                null,
                                valorNuevo);

                super.publicarTransaccion(dto, "transaccion");
        }

        /**
         * Publica un evento de acceso o lectura de información sensible.
         * <p>
         * Se utiliza para registrar consultas de historial, resúmenes financieros y
         * reportes.
         * </p>
         * 
         * @param usuarioId ID del usuario que accede a la información.
         * @param accion    Etiqueta de la acción realizada (ej: CONSULTA_HISTORIAL).
         * @param mensaje   Detalle descriptivo o rango de búsqueda.
         * @param ip        Dirección IP de origen.
         */
        public void publicarAcceso(UUID usuarioId, String accion, String mensaje, String ip) {
                // Combinamos la acción con el mensaje para no perder metadata en el DTO oficial
                String detalleCompleto = String.format("[%s] %s", accion, mensaje);

                EventoAccesoDTO dto = EventoAccesoDTO.de(
                                usuarioId,
                                ip,
                                EstadoEvento.EXITO,
                                detalleCompleto,
                                null);

                super.publicarAcceso(dto, EstadoEvento.EXITO);
        }
}