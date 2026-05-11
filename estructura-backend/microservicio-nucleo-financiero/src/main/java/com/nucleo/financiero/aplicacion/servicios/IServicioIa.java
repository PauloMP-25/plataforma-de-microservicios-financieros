package com.nucleo.financiero.aplicacion.servicios;

import com.libreria.comun.dtos.RespuestaIaDTO;
import com.libreria.comun.dtos.SolicitudIaDTO;

/**
 * Interfaz de servicio para la interacción con el motor de Inteligencia Artificial.
 * <p>
 * Define el contrato para solicitar análisis y consejos financieros basados
 * en el comportamiento del usuario.
 * </p>
 *
 * @author Luka-Dev-Backend
 * @version 1.1.0
 */
public interface IServicioIa {

    /**
     * Procesa una solicitud de consejo financiero, enriqueciéndola con contexto
     * y delegando el análisis al microservicio de IA.
     *
     * @param solicitud Datos básicos de la petición.
     * @param ipCliente Dirección IP de origen para registro de auditoría.
     * @return DTO con el consejo generado y metadatos del análisis.
     */
    RespuestaIaDTO obtenerConsejoIA(SolicitudIaDTO solicitud, String ipCliente);
}
