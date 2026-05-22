package com.libreria.comun.autoconfiguracion;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.HealthComponent;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.health.SystemHealth;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class ControladorSaludCustom {

    private final HealthEndpoint healthEndpoint;
    private final Environment environment;

    @Value("${spring.application.name:Microservicio}")
    private String applicationName;

    @Value("${info.app.version:1.0.0}")
    private String version;

    public ControladorSaludCustom(HealthEndpoint healthEndpoint, Environment environment) {
        this.healthEndpoint = healthEndpoint;
        this.environment = environment;
    }

    @GetMapping("/actuator/health")
    public ResponseEntity<Map<String, Object>> customHealth() {
        HealthComponent health = healthEndpoint.health();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", health.getStatus().getCode());
        response.put("estado", health.getStatus().getCode());
        
        String formattedServiceName = "Microservicio " + applicationName.replace("microservicio-", "").toUpperCase() + " - LUKA";
        response.put("servicio", formattedServiceName);
        response.put("version", version);
        
        String[] activeProfiles = environment.getActiveProfiles();
        String entorno = activeProfiles.length > 0 ? activeProfiles[0] : "desarrollo";
        response.put("entorno", entorno);
        
        response.put("timestamp", LocalDateTime.now().toString());

        if (health instanceof SystemHealth systemHealth) {
            Map<String, Object> componentes = new LinkedHashMap<>();
            systemHealth.getComponents().forEach((key, component) -> {
                Map<String, Object> compDetails = new LinkedHashMap<>();
                compDetails.put("estado", component.getStatus().getCode());
                
                String status = component.getStatus().getCode();
                String descBase = "UP".equals(status) ? "operativo" : "con problemas";

                // Map standard actuator indicators to custom descriptions
                if ("db".equals(key) || "postgresql".equalsIgnoreCase(key)) {
                    compDetails.put("descripcion", "Base de datos PostgreSQL " + descBase);
                } else if ("rabbit".equals(key) || "rabbitmq".equalsIgnoreCase(key)) {
                    compDetails.put("descripcion", "Broker RabbitMQ " + descBase);
                } else if ("redis".equals(key)) {
                    compDetails.put("descripcion", "Caché Redis " + descBase);
                } else if ("mail".equals(key)) {
                    compDetails.put("descripcion", "Servicio de correo " + descBase);
                } else if ("stripe".equalsIgnoreCase(key)) {
                    compDetails.put("descripcion", "Pasarela Stripe " + descBase);
                } else if ("twilio".equalsIgnoreCase(key)) {
                    compDetails.put("descripcion", "Servicio Twilio " + descBase);
                } else if ("gemini".equalsIgnoreCase(key)) {
                    compDetails.put("descripcion", "IA Gemini " + descBase);
                } else if ("diskSpace".equals(key)) {
                    compDetails.put("descripcion", "UP".equals(status) ? "Espacio en disco suficiente" : "Problemas de espacio en disco");
                } else if ("ping".equals(key)) {
                    compDetails.put("descripcion", "UP".equals(status) ? "Ping exitoso" : "Fallo en ping");
                } else if ("discoveryComposite".equals(key) || "eureka".equalsIgnoreCase(key)) {
                    compDetails.put("descripcion", "Conexión con Eureka " + descBase);
                } else if ("refreshScope".equals(key)) {
                    compDetails.put("descripcion", "Refresh Scope " + descBase);
                } else {
                    compDetails.put("descripcion", "Componente " + key + " " + descBase);
                }
                componentes.put(key, compDetails);
            });
            response.put("componentes", componentes);
            response.put("endpoints_disponibles", componentes.size() + 5); 
        } else {
            response.put("componentes", Map.of());
            response.put("endpoints_disponibles", 5);
        }

        if ("UP".equals(health.getStatus().getCode())) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(503).body(response);
        }
    }
}
