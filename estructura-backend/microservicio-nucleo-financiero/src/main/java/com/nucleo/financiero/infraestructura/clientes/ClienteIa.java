package com.nucleo.financiero.infraestructura.clientes;

import com.libreria.comun.dtos.RespuestaIaDTO;
import com.libreria.comun.dtos.SolicitudIaDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Cliente Feign para la comunicación con el motor de Inteligencia Artificial (Python/FastAPI).
 * <p>
 * Este cliente facilita la integración síncrona con el servicio de IA para obtener
 * recomendaciones financieras en tiempo real.
 * </p>
 * 
 * @author Luka-Dev-Backend
 */
@FeignClient(name = "microservicio-ia", url = "${microservicio.ia.url:http://localhost:8086}")
public interface ClienteIa {

    /**
     * Envía una solicitud de análisis financiero al motor de IA.
     * 
     * @param solicitud DTO enriquecido con el historial y contexto del usuario.
     * @return DTO con el consejo generado por Gemini.
     */
    @PostMapping("/api/v1/ia/analizar")
    RespuestaIaDTO analizarFinanzas(@RequestBody SolicitudIaDTO solicitud);
}
