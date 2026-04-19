package com.usuario.infraestructura.clientes;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

/**
 * Cliente HTTP para comunicación síncrona con el Microservicio-Cliente.
 * Responsabilidad única: solicitar la creación de un perfil inicial en blanco.
 * La llamada es tolerante a fallos: si el servicio remoto no responde,
 * se loguea el error pero NO se propaga (la activación de cuenta ya ocurrió).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ClientePerfilExterno {

    private final RestTemplate restTemplate;

    @Value("${cliente.service.perfil-inicial.url}")
    private String urlPerfilInicial;

    /**
     * Solicita al Microservicio-Cliente la creación de un perfil vacío
     * asociado al usuarioId recién activado.
     *
     * @param usuarioId UUID del usuario cuya cuenta acaba de ser confirmada.
     */
    public void crearPerfilInicial(UUID usuarioId) {
        try {
            URI uri = UriComponentsBuilder
                    .fromUriString(urlPerfilInicial)
                    .queryParam("usuarioId", usuarioId)
                    .build()
                    .toUri();

            ResponseEntity<Void> respuesta = restTemplate.postForEntity(uri, null, Void.class);

            if (respuesta.getStatusCode().is2xxSuccessful()) {
                log.info("[CLIENTE-EXTERNO] Perfil inicial creado para usuarioId={}", usuarioId);
            } else {
                log.warn("[CLIENTE-EXTERNO] Respuesta inesperada al crear perfil: status={}, usuarioId={}",
                        respuesta.getStatusCode(), usuarioId);
            }

        } catch (RestClientException ex) {
            // ⚠️ Tolerancia a fallos: no revertir la activación de cuenta
            log.error("[CLIENTE-EXTERNO] Fallo al crear perfil inicial para usuarioId={}. " +
                      "La cuenta fue activada correctamente. Error: {}", usuarioId, ex.getMessage());
        }
    }
}
