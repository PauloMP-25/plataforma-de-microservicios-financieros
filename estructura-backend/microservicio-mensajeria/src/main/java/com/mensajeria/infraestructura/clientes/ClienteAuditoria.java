package com.mensajeria.infraestructura.clientes;


import com.mensajeria.aplicacion.dtos.RegistroAuditoriaDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * Cliente HTTP asíncrono para reportar eventos al Microservicio-Auditoría.
 * El envío es no bloqueante: un fallo de auditoría nunca interrumpe el flujo principal.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ClienteAuditoria {

    private final RestTemplate restTemplate;

    @Value("${auditoria.service.url:http://localhost:8082/api/v1/auditoria/registrar}")
    private String urlAuditoria;

    /**
     * Envía el evento al microservicio-auditoría de forma ASÍNCRONA.
     * Requiere ser llamado desde un bean externo al que lo define (Spring proxy).
     * @param solicitud
     */
    @Async
    public void enviar(RegistroAuditoriaDTO solicitud) {
        try {
            restTemplate.postForEntity(urlAuditoria, solicitud, Void.class);
            log.debug("[AUDITORIA] Evento enviado: accion={}, usuario={}",
                      solicitud.accion(), solicitud.nombreUsuario());
        } catch (RestClientException ex) {
            // No propagamos: la auditoría es informativa, no bloquea el flujo
            log.error("[AUDITORIA] Fallo al enviar evento (no bloqueante): {}", ex.getMessage());
        }
    }
}

