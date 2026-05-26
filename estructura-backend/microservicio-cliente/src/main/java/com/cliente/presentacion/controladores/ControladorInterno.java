package com.cliente.presentacion.controladores;

import com.libreria.comun.dtos.ContextoEstrategicoIADTO;
import com.libreria.comun.dtos.ContextoUsuarioDTO;
import com.libreria.comun.respuesta.ResultadoApi;
import com.cliente.aplicacion.puertos.ServicioContexto;
import com.cliente.aplicacion.puertos.ServicioDatosPersonales;
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
@org.springframework.security.access.prepost.PreAuthorize("@seguridadService.esServicioInterno()")
public class ControladorInterno {

    private final ServicioContexto servicioContexto;
    private final ServicioDatosPersonales servicioDatosPersonales;

    /**
     * Retorna el contexto financiero y personal optimizado (menor privilegio)
     * para la generación de recomendaciones en el ms-ia.
     */
    @GetMapping("/contexto-financiero/{usuarioId}")
    public ResponseEntity<ResultadoApi<ContextoEstrategicoIADTO>> obtenerContextoFinanciero(
            @PathVariable UUID usuarioId) {
        log.debug("Solicitud interna de contexto estratégico de IA para usuarioId={}", usuarioId);
        return ResponseEntity.ok(ResultadoApi.exito(servicioContexto.obtenerContextoFinanciero(usuarioId), "Contexto estratégico de IA recuperado", null));
    }

    /**
     * Retorna el contexto consolidado completo del usuario.
     */
    @GetMapping("/contexto/{usuarioId}")
    public ResponseEntity<ResultadoApi<ContextoUsuarioDTO>> obtenerContexto(
            @PathVariable UUID usuarioId) {
        log.debug("Solicitud interna de contexto completo para usuarioId={}", usuarioId);
        return ResponseEntity.ok(ResultadoApi.exito(servicioContexto.obtenerContextoCompleto(usuarioId), "Contexto completo recuperado", null));
    }

    /**
     * Crea el perfil inicial para un nuevo usuario registrado.
     */
    @PostMapping("/inicial")
    public ResponseEntity<ResultadoApi<Void>> crearPerfilInicial(@RequestParam UUID usuarioId) {
        log.info("[INTERNO] Solicitud de creación de perfil inicial para usuario: {}", usuarioId);
        servicioDatosPersonales.crearPerfil(usuarioId);
        return ResponseEntity.ok().body(ResultadoApi.sinContenido("Perfil inicial creado con éxito."));
    }

    /**
     * Recupera el teléfono verificado de un usuario.
     */
    @GetMapping("/perfiles/{usuarioId}/telefono")
    public ResponseEntity<ResultadoApi<String>> obtenerTelefono(
            @PathVariable UUID usuarioId) {
        log.info("[INTERNO] Recuperando teléfono para usuario: {}", usuarioId);
        com.cliente.aplicacion.dtos.respuestas.RespuestaDatosPersonales respuesta = servicioDatosPersonales.consultarInterno(usuarioId);
        String telefono = respuesta != null ? respuesta.telefono() : null;
        return ResponseEntity.ok(ResultadoApi.exito(telefono, "Teléfono recuperado", null));
    }

    /**
     * Actualiza el teléfono de un usuario tras una validación exitosa en el
     * ms-mensajeria.
     */
    @PatchMapping("/perfiles/{usuarioId}/telefono")
    public ResponseEntity<ResultadoApi<Void>> actualizarTelefono(
            @PathVariable UUID usuarioId,
            @RequestParam String telefono) {
        log.info("[INTERNO] Sincronizando teléfono para usuario: {} -> {}", usuarioId, telefono);
        servicioDatosPersonales.actualizarTelefono(usuarioId, telefono);
        return ResponseEntity.ok().body(ResultadoApi.sinContenido("Teléfono sincronizado con éxito."));
    }
}