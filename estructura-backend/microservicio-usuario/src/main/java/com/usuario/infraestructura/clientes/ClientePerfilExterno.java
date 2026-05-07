package com.usuario.infraestructura.clientes;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.UUID;

/**
 * Cliente Feign para comunicación síncrona con el Microservicio-Cliente.
 * Responsabilidad única: solicitar la creación de un perfil inicial en blanco.
 * La llamada es tolerante a fallos: si el servicio remoto no responde,
 * se loguea el error pero NO se propaga (la activación de cuenta ya ocurrió).
 */
@FeignClient(
    name = "microservicio-cliente", 
    url = "${microservicio.cliente.url:http://localhost:8083}",
    fallback = ClientePerfilExternoFallback.class
)
public interface ClientePerfilExterno {

    @PostMapping("/api/v1/clientes/inicial")
    void crearPerfilInicial(@RequestParam("usuarioId") UUID usuarioId);
    
    // Nuevo: Para guardar el teléfono cuando el usuario lo elige en la activación
    @PatchMapping("/api/perfiles/{usuarioId}/telefono")
    void actualizarTelefono(@PathVariable UUID usuarioId, @RequestParam String telefono);

    // Nuevo: Para recuperar el teléfono en el flujo de "olvidé mi contraseña"
    @GetMapping("/api/perfiles/{usuarioId}/telefono")
    String obtenerTelefono(@PathVariable UUID usuarioId);
}