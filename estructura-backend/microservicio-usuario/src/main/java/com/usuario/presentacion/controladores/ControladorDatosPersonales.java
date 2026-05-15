package com.usuario.presentacion.controladores;

import com.libreria.comun.respuesta.ResultadoApi;
import com.usuario.aplicacion.servicios.IServicioAutenticacion;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Controlador para la gestión de datos personales del usuario.
 * Proporciona endpoints para la sincronización de información verificada.
 */
@RestController
@RequestMapping("/api/v1/datos-personales")
@RequiredArgsConstructor
@Slf4j
public class ControladorDatosPersonales {

    private final IServicioAutenticacion servicioAuth;

    /**
     * Sincroniza el número de teléfono verificado de un usuario.
     * Invocado usualmente por el ms-mensajeria tras una validación OTP exitosa.
     *
     * @param usuarioId UUID del usuario.
     * @param telefono  Número de teléfono verificado.
     * @return Respuesta exitosa con mensaje de confirmación.
     */
    @PutMapping("/telefono/{usuarioId}")
    public ResponseEntity<ResultadoApi<String>> sincronizarTelefono(
            @PathVariable UUID usuarioId,
            @RequestParam String telefono) {
        
        log.info("[API-SYNC] Solicitud de sincronización de teléfono para usuario: {}", usuarioId);
        servicioAuth.sincronizarTelefono(usuarioId, telefono);
        
        return ResponseEntity.ok(ResultadoApi.exito("OK", "Teléfono sincronizado correctamente."));
    }
}
