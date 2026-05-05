package com.nucleo.financiero.aplicacion.servicios;

import com.nucleo.financiero.aplicacion.dtos.cliente.ContextoUsuarioDTO;
import com.nucleo.financiero.aplicacion.dtos.ia.RespuestaIaDTO;
import com.nucleo.financiero.aplicacion.dtos.ia.SolicitudIaDTO;
import com.nucleo.financiero.infraestructura.clientes.ClienteIa;
import com.nucleo.financiero.infraestructura.mensajeria.PublicadorAuditoria;
import com.nucleo.financiero.infraestructura.clientes.ClienteContexto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class ServicioIa {

    private final ClienteIa clienteIa;
    private final ClienteContexto clienteContexto;
    private final PublicadorAuditoria publicadorAuditoria;

    /**
     * Obtiene un consejo personalizado de la IA, enriqueciendo la solicitud con
     * el contexto del cliente y registrando la actividad en la auditoría
     * mediante RabbitMQ.
     *
     * @param solicitud Datos básicos de la petición.
     * @param ipCliente IP real capturada desde el controlador.
     * @return Respuesta procesada por Gemini en Python.
     */
    public RespuestaIaDTO obtenerConsejoIA(SolicitudIaDTO solicitud, String ipCliente) {
        log.info("Iniciando proceso de IA para el usuario: {} desde IP: {}", solicitud.getIdUsuario(), ipCliente);

        // 1. Obtener contexto completo del cliente (Datos personales, perfil, metas, límites)
        ContextoUsuarioDTO contextoEnriquecido = clienteContexto.obtenerContexto(solicitud.getIdUsuario());

        // 2. Re-construir la solicitud usando tus métodos de fábrica para incluir el contexto
        // Esto mantiene la inmutabilidad de tu @Value SolicitudIaDTO
        SolicitudIaDTO solicitudFinal;

        if (solicitud.getModuloSolicitado() != null) {
            solicitudFinal = SolicitudIaDTO.paraConsultaModulo(
                    solicitud.getIdUsuario(),
                    solicitud.getModuloSolicitado(),
                    solicitud.getHistorialMensual(),
                    contextoEnriquecido
            );
        } else {
            solicitudFinal = SolicitudIaDTO.paraTransaccionReciente(
                    solicitud.getIdUsuario(),
                    solicitud.getHistorialMensual(),
                    contextoEnriquecido
            );
        }

        // 3. Llamada síncrona al microservicio de IA (Python - FastAPI) vía Feign
        log.debug("Enviando solicitud enriquecida a Python para análisis...");
        RespuestaIaDTO respuesta = clienteIa.analizarFinanzas(solicitudFinal);

        // 4. Registro Asíncrono en Auditoría vía RabbitMQ
        // Usamos 'publicarAcceso' para indicar que se consumió información analítica.
        publicadorAuditoria.publicarAcceso(
                solicitudFinal.getIdUsuario(),
                "CONSULTA_IA",
                "Análisis generado con contexto: " + (solicitudFinal.getModuloSolicitado() != null
                ? solicitudFinal.getModuloSolicitado() : "TRANSACCION_RECIENTE"),
                ipCliente
        );

        return respuesta;
    }
}
