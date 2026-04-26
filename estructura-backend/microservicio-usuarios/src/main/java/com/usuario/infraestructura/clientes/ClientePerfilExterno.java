package com.usuario.infraestructura.clientes;

import org.springframework.cloud.openfeign.FeignClient;
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
    url = "${microservicio.cliente.url:http://localhost:8083}"
)
public interface ClientePerfilExterno {

    @PostMapping("/api/v1/perfiles/inicial")
    void crearPerfilInicial(@RequestParam("usuarioId") UUID usuarioId);
}