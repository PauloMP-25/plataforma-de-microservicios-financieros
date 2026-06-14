package com.usuario.presentacion.controladores;

import com.libreria.comun.respuesta.ResultadoApi;
import com.usuario.aplicacion.puertos.IServicioAutenticacion;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Datos Personales (Profile Sync)", description = "Controlador para la gestión de la información de perfil y sincronización de datos personales verificados con otros microservicios.")
public class ControladorDatosPersonales {

    private final IServicioAutenticacion servicioAuth;

    /**
     * Sincroniza el número de teléfono verificado de un usuario.
     * Invocado usualmente por el ms-mensajeria tras una validación OTP exitosa.
     */
    @PutMapping("/telefono/{usuarioId}")
    @Operation(summary = "Sincronizar Teléfono Verificado", description = "Registra y asocia un número telefónico validado a la cuenta del usuario. Invocado principalmente por el canal de mensajería OTP tras una confirmación exitosa.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Teléfono sincronizado e integrado correctamente en el perfil del usuario."),
        @ApiResponse(responseCode = "400", description = "Parámetros inválidos o formato telefónico incorrecto."),
        @ApiResponse(responseCode = "404", description = "Usuario no encontrado en la base de datos.")
    })
    public ResponseEntity<ResultadoApi<String>> sincronizarTelefono(
            @PathVariable @Parameter(description = "UUID único del usuario.", example = "d3b07384-d113-4a0b-8083-d922a901ba8d") UUID usuarioId,
            @RequestParam @Parameter(description = "Número telefónico verificado a asociar.", example = "+51999999999") String telefono) {
        
        log.info("[API-SYNC] Solicitud de sincronización de teléfono para usuario: {}", usuarioId);
        servicioAuth.sincronizarTelefono(usuarioId, telefono);
        
        return ResponseEntity.ok(ResultadoApi.exito("OK", "Teléfono sincronizado correctamente."));
    }
}
