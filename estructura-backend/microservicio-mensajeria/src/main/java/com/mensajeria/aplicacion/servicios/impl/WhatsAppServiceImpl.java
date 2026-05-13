package com.mensajeria.aplicacion.servicios.impl;

import com.mensajeria.aplicacion.excepciones.MensajeriaExternaException;
import com.mensajeria.aplicacion.servicios.IWhatsAppService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementación del servicio de WhatsApp usando Meta Cloud API.
 * <p>
 * Se encarga de la construcción de la carga útil JSON requerida por Meta
 * y el manejo de la autenticación mediante Bearer Token.
 * </p>
 *
 * @author Paulo Moron
 * @version 1.0.0
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class WhatsAppServiceImpl implements IWhatsAppService, com.mensajeria.aplicacion.servicios.CanalNotificacionStrategy {

    @Override
    public void enviar(String destinatario, Map<String, Object> variables) {
        String codigo = (String) variables.get("codigo");
        // Para WhatsApp usamos la plantilla de autenticación
        Map<String, String> waVariables = Map.of("1", codigo);
        this.enviarMensajeTemplate(destinatario, "otp_verification", waVariables);
    }

    @Override
    public boolean soporta(com.mensajeria.aplicacion.servicios.TipoNotificacion tipo) {
        return tipo == com.mensajeria.aplicacion.servicios.TipoNotificacion.WHATSAPP;
    }


    private final RestTemplate restTemplate;

    @Value("${whatsapp.api.token:}")
    private String apiToken;

    @Value("${whatsapp.api.phone-number-id:}")
    private String phoneNumberId;

    @Value("${whatsapp.api.version:v19.0}")
    private String apiVersion;

    private static final String META_URL_TEMPLATE = "https://graph.facebook.com/%s/%s/messages";

    @SuppressWarnings("null")
    @Override
    public void enviarMensajeTemplate(String telefono, String plantilla, Map<String, String> variables) {
        if (apiToken == null || apiToken.isEmpty() || phoneNumberId == null || phoneNumberId.isEmpty()) {
            log.warn("[WHATSAPP] Credenciales no configuradas. Saltando envío.");
            return;
        }

        try {
            String url = String.format(META_URL_TEMPLATE, apiVersion, phoneNumberId);
            
            // Construcción del JSON para Meta Cloud API
            Map<String, Object> body = new HashMap<>();
            body.put("messaging_product", "whatsapp");
            body.put("to", limpiarTelefono(telefono));
            body.put("type", "template");

            Map<String, Object> template = new HashMap<>();
            template.put("name", plantilla);
            
            Map<String, String> language = new HashMap<>();
            language.put("code", "es"); // Por defecto español
            template.put("language", language);

            // Componentes (Placeholders {{1}}, {{2}}, etc)
            if (variables != null && !variables.isEmpty()) {
                List<Map<String, Object>> components = new ArrayList<>();
                Map<String, Object> bodyComponent = new HashMap<>();
                bodyComponent.put("type", "body");
                
                List<Map<String, String>> parameters = new ArrayList<>();
                // Se asume que las variables vienen en orden "1", "2", etc o se mapean
                variables.forEach((k, v) -> {
                    Map<String, String> param = new HashMap<>();
                    param.put("type", "text");
                    param.put("text", v);
                    parameters.add(param);
                });
                
                bodyComponent.put("parameters", parameters);
                components.add(bodyComponent);
                template.put("components", components);
            }

            body.put("template", template);

            // Headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiToken);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("[WHATSAPP] Mensaje enviado exitosamente a {}. Respuesta: {}", telefono, response.getBody());
            } else {
                log.error("[WHATSAPP] Error de Meta API: {} - {}", response.getStatusCode(), response.getBody());
                throw new MensajeriaExternaException("Meta API rechazó el mensaje", response.getBody());
            }

        } catch (Exception e) {
            log.error("[WHATSAPP] Fallo crítico al enviar mensaje a {}: {}", telefono, e.getMessage());
            throw new MensajeriaExternaException("Error de comunicación con WhatsApp Cloud API", e.getMessage());
        }
    }

    @Override
    public boolean esNumeroValido(String telefono) {
        // WhatsApp requiere formato E.164 sin el símbolo '+' para Meta API
        return telefono != null && telefono.matches("^\\d{10,15}$");
    }

    private String limpiarTelefono(String telefono) {
        if (telefono == null) return "";
        return telefono.replace("+", "").replace(" ", "");
    }
}
