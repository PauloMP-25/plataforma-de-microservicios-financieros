package com.nucleo.financiero.infraestructura.mensajeria;

import com.libreria.comun.mensajeria.NombresExchange;
import com.libreria.comun.dtos.RespuestaIaDTO;
import com.libreria.comun.dtos.SolicitudIaDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PublicadorIA {

    private final RabbitTemplate rabbitTemplate;

    /**
     * Este método recupera la esencia de tu antiguo 'PublicadorTransaccionIA'.
     * Envía el resultado del análisis de Gemini a la cola para que otros 
     * servicios (como Notificaciones o Reportes) puedan reaccionar.
     * @param solicitud
     * @param respuesta
     */
    public void publicarResultadoAnalisis(SolicitudIaDTO solicitud, RespuestaIaDTO respuesta) {
        // Aquí podrías crear un DTO específico si lo deseas, 
        // o enviar la respuesta directamente.
        rabbitTemplate.convertAndSend(
                NombresExchange.IA,
            "ia.analisis.resultado", // Una RK específica para resultados
            respuesta
        );
    }
}
