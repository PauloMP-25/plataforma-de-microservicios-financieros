package com.nucleo.financiero.aplicacion.servicios.implementacion;

import com.libreria.comun.dtos.ContextoUsuarioDTO;
import com.libreria.comun.dtos.RespuestaIaDTO;
import com.libreria.comun.dtos.SolicitudIaDTO;
import com.libreria.comun.enums.TipoSolicitudIa;
import com.nucleo.financiero.aplicacion.servicios.IServicioIa;
import com.nucleo.financiero.infraestructura.clientes.ClienteIa;
import com.nucleo.financiero.infraestructura.mensajeria.PublicadorAuditoria;
import com.nucleo.financiero.infraestructura.clientes.ClienteContexto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Implementación de {@link IServicioIa} para la interacción con el ecosistema de Inteligencia Artificial.
 * <p>
 * Coordina la recuperación de contexto del cliente, el enriquecimiento de solicitudes
 * y la comunicación con el motor de IA basado en Python. Además, registra la actividad
 * analítica en el sistema de auditoría.
 * </p>
 *
 * @author Luka-Dev-Backend
 * @version 1.2.0
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ServicioIaImpl implements IServicioIa {

    private final ClienteIa clienteIa;
    private final ClienteContexto clienteContexto;
    private final PublicadorAuditoria publicadorAuditoria;

    @Override
    public RespuestaIaDTO obtenerConsejoIA(SolicitudIaDTO solicitud, String ipCliente) {
        log.info("Iniciando proceso de IA para el usuario: {} desde IP: {}", solicitud.getUsuarioId(), ipCliente);

        // 1. Obtener contexto completo del cliente (Datos personales, perfil, metas, límites)
        ContextoUsuarioDTO contextoEnriquecido = clienteContexto.obtenerContexto(solicitud.getUsuarioId());

        // 2. Re-construir la solicitud enriquecida con el contexto recuperado
        SolicitudIaDTO solicitudFinal;

        if (solicitud.getModuloSolicitado() != null) {
            solicitudFinal = SolicitudIaDTO.builder()
                    .usuarioId(solicitud.getUsuarioId())
                    .tipoSolicitud(TipoSolicitudIa.CONSULTA_MODULO)
                    .moduloSolicitado(solicitud.getModuloSolicitado())
                    .historialMensual(solicitud.getHistorialMensual())
                    .contexto(contextoEnriquecido)
                    .build();
        } else {
            solicitudFinal = SolicitudIaDTO.builder()
                    .usuarioId(solicitud.getUsuarioId())
                    .tipoSolicitud(TipoSolicitudIa.TRANSACCION_RECIENTE)
                    .historialMensual(solicitud.getHistorialMensual())
                    .contexto(contextoEnriquecido)
                    .build();
        }

        // 3. Llamada síncrona al microservicio de IA (Python - FastAPI) vía Feign
        log.debug("Enviando solicitud enriquecida a Python para análisis...");
        RespuestaIaDTO respuesta = clienteIa.analizarFinanzas(solicitudFinal);

        // 4. Registro Asíncrono en Auditoría vía RabbitMQ
        publicadorAuditoria.publicarAcceso(
                solicitudFinal.getUsuarioId(),
                "CONSULTA_IA",
                "Análisis generado con contexto: " + (solicitudFinal.getModuloSolicitado() != null
                ? solicitudFinal.getModuloSolicitado() : "TRANSACCION_RECIENTE"),
                ipCliente
        );

        return respuesta;
    }
}
