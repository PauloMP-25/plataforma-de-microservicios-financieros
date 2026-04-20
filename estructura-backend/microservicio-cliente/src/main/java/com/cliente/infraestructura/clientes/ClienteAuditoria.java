package com.cliente.infraestructura.clientes;

import com.cliente.aplicacion.dtos.RegistroAuditoriaDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
@RequiredArgsConstructor
public class ClienteAuditoria {

    private final RestTemplate restTemplate;

    @Value("${auditoria.service.url:http://localhost:8082/api/v1/auditoria/registrar}")
    private String urlAuditoria;

    /**
     * Envía el evento al microservicio-auditoría de forma ASÍNCRONA.
     * @param solicitud
     * @Async requiere que la llamada sea desde un bean diferente al que la define
     * (Spring crea un proxy — no llamar desde el mismo bean).
     */
    @Async
    public void enviar(RegistroAuditoriaDTO solicitud) {
        try {
            restTemplate.postForEntity(urlAuditoria, solicitud, Void.class);
            log.debug("[MICROSERVICIO.AUDITORIA] Evento enviado: accion={} usuario={}",
                      solicitud.accion(), solicitud.nombreUsuario());
        } catch (RestClientException ex) {
            //No propagar: la auditoría es informativa, no bloquea el flujo
            log.error("[MICROSERVICIO-AUDITORIA] Fallo al enviar evento (no bloqueante): {}", ex.getMessage());
        }
    }
}
