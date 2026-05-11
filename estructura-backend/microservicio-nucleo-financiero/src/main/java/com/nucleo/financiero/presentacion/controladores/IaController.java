package com.nucleo.financiero.presentacion.controladores;

import com.libreria.comun.dtos.RespuestaIaDTO;
import com.libreria.comun.dtos.SolicitudIaDTO;
import com.libreria.comun.respuesta.ResultadoApi;
import com.nucleo.financiero.aplicacion.servicios.IServicioIa;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador REST para la interacción con el motor de Inteligencia Artificial
 * (LUKA-IA).
 * <p>
 * Este controlador expone endpoints para solicitar consejos financieros
 * personalizados
 * basados en el comportamiento transaccional del usuario. Utiliza el contrato
 * {@link IServicioIa}
 * para delegar el procesamiento analítico.
 * </p>
 *
 * @author Luka-Dev-Backend
 * @version 1.2.2
 */
@RestController
@RequestMapping("/api/v1/ia")
@RequiredArgsConstructor
@Slf4j
public class IaController {

    private final IServicioIa servicioIa;

    /**
     * Consulta al motor de IA para obtener un consejo financiero estratégico.
     * <p>
     * El flujo de negocio incluye:
     * 1. Extracción de la IP del cliente para auditoría.
     * 2. Recuperación del contexto financiero del usuario.
     * 3. Generación de prompt dinámico y consulta a Gemini.
     * 4. Registro del evento en el microservicio de auditoría vía RabbitMQ.
     * </p>
     * 
     * @param solicitud      Datos de la consulta (usuarioId, contexto opcional).
     * @param servletRequest Petición HTTP para extracción de metadatos (IP).
     * @return ResponseEntity con {@link ResultadoApi} conteniendo la respuesta de
     *         la IA.
     */
    @PostMapping("/consultar")
    public ResponseEntity<ResultadoApi<RespuestaIaDTO>> consultarIa(
            @Valid @RequestBody SolicitudIaDTO solicitud,
            HttpServletRequest servletRequest) {

        log.info("Iniciando consulta de IA para usuarioId={}", solicitud.getIdUsuario());

        // Extraemos la IP real del cliente para trazabilidad
        String ipCliente = servletRequest.getHeader("X-Forwarded-For");
        if (ipCliente == null) {
            ipCliente = servletRequest.getRemoteAddr();
        }

        RespuestaIaDTO respuesta = servicioIa.obtenerConsejoIA(solicitud, ipCliente);

        return ResponseEntity.ok(ResultadoApi.exito(respuesta, "Consejo de IA generado exitosamente"));
    }
}
