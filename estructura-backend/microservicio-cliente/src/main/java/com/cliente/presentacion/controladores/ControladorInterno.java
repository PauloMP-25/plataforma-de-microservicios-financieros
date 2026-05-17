package com.cliente.presentacion.controladores;

import com.libreria.comun.dtos.ContextoEstrategicoIADTO;
import com.cliente.aplicacion.servicios.ServicioContexto;
import com.cliente.aplicacion.servicios.ServicioDatosPersonales;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Controlador INTERNO único para comunicación y sincronización entre
 * microservicios vía Feign.
 *
 * Expone el contexto del cliente y maneja eventos de sincronización del perfil
 * del usuario.
 * No requiere autenticación JWT del usuario final ya que es comunicación
 * interna de confianza.
 *
 * @author Paulo Moron
 * @version 1.2.0
 * @since 2026-05-10
 */
@RestController
@RequestMapping("/api/v1/clientes/interno")
@RequiredArgsConstructor
@Slf4j
public class ControladorInterno {

    private final ServicioContexto servicioContexto;
    private final ServicioDatosPersonales servicioDatosPersonales;

    /**
     * Retorna el contexto financiero y personal optimizado (menor privilegio)
     * para la generación de recomendaciones en el ms-ia.
     * 
     * @param usuarioId ID del usuario
     * @return DTO ligero con el contexto para IA.
     */
    @GetMapping("/contexto-financiero/{usuarioId}")
    public ResponseEntity<ContextoEstrategicoIADTO> obtenerContextoFinanciero(
            @PathVariable UUID usuarioId) {
        log.debug("Solicitud interna de contexto estratégico de IA para usuarioId={}", usuarioId);
        return ResponseEntity.ok(servicioContexto.obtenerContextoFinanciero(usuarioId));
    }

    /**
     * Retorna el contexto consolidado completo del usuario.
     * Utilizado por ms-nucleo-financiero.
     * 
     * @param usuarioId ID del usuario
     * @return DTO completo del contexto de usuario.
     */
    @GetMapping("/contexto/{usuarioId}")
    public ResponseEntity<com.libreria.comun.dtos.ContextoUsuarioDTO> obtenerContexto(
            @PathVariable UUID usuarioId) {
        log.debug("Solicitud interna de contexto completo para usuarioId={}", usuarioId);
        return ResponseEntity.ok(servicioContexto.obtenerContextoCompleto(usuarioId));
    }

    /**
     * Crea el perfil inicial para un nuevo usuario registrado.
     * 
     * @param usuarioId ID del usuario a inicializar
     * @return Respuesta vacía 200 OK
     */
    @PostMapping("/inicial")
    public ResponseEntity<Void> crearPerfilInicial(@RequestParam UUID usuarioId) {
        log.info("[INTERNO] Solicitud de creación de perfil inicial para usuario: {}", usuarioId);
        servicioDatosPersonales.crearPerfil(usuarioId);
        return ResponseEntity.ok().build();
    }

    /**
     * Actualiza el teléfono de un usuario tras una validación exitosa en el
     * ms-mensajeria.
     * 
     * @param usuarioId ID del usuario
     * @param telefono  Nuevo número telefónico validado
     * @return Respuesta vacía 200 OK
     */
    @PatchMapping("/perfiles/{usuarioId}/telefono")
    public ResponseEntity<Void> actualizarTelefono(
            @PathVariable UUID usuarioId,
            @RequestParam String telefono) {
        log.info("[INTERNO] Sincronizando teléfono para usuario: {} -> {}", usuarioId, telefono);
        servicioDatosPersonales.actualizarTelefono(usuarioId, telefono);
        return ResponseEntity.ok().build();
    }
}