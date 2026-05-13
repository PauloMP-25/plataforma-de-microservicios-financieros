package com.cliente.presentacion.controladores;

import com.cliente.aplicacion.servicios.ServicioDatosPersonales;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Controlador para la sincronización de datos entre microservicios.
 * Este controlador no requiere JWT del usuario ya que es comunicación interna de confianza (vía Feign).
 */
@RestController
@RequestMapping("/api/v1/clientes/interno")
@RequiredArgsConstructor
@Slf4j
public class ControladorSincronizacionInterna {

    private final ServicioDatosPersonales servicio;

    /**
     * Crea el perfil inicial para un nuevo usuario registrado.
     */
    @PostMapping("/inicial")
    public ResponseEntity<Void> crearPerfilInicial(@RequestParam UUID usuarioId) {
        log.info("[INTERNO] Solicitud de creación de perfil inicial para usuario: {}", usuarioId);
        servicio.crearPerfil(usuarioId);
        return ResponseEntity.ok().build();
    }

    /**
     * Actualiza el teléfono de un usuario tras una validación exitosa en el ms-mensajeria.
     */
    @PatchMapping("/perfiles/{usuarioId}/telefono")
    public ResponseEntity<Void> actualizarTelefono(
            @PathVariable UUID usuarioId, 
            @RequestParam String telefono) {
        log.info("[INTERNO] Sincronizando teléfono para usuario: {} -> {}", usuarioId, telefono);
        servicio.actualizarTelefono(usuarioId, telefono);
        return ResponseEntity.ok().build();
    }
}
