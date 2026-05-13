package com.cliente.presentacion.controladores;

import com.libreria.comun.dtos.ContextoEstrategicoIADTO;
import com.cliente.aplicacion.servicios.ServicioContexto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Controlador INTERNO para comunicación entre microservicios vía Feign.
 *
 * Expone el contexto completo del cliente al microservicio-nucleo-financiero.
 * No requiere autenticación JWT del usuario final, pero debería estar
 * protegido por seguridad de red (mismo cluster / API Gateway interno).
 *
 * Ruta:
 * GET /api/v1/clientes/interno/contexto/{usuarioId}
 *
 * Interfaz Feign sugerida en microservicio-nucleo-financiero:
 * 
 * <pre>
 * {@code
 * @FeignClient(name = "microservicio-cliente")
 * public interface ClienteContextoFeign {
 *
 *     @GetMapping("/api/v1/clientes/interno/contexto/{usuarioId}")
 *     RespuestaContexto obtenerContexto(@PathVariable UUID usuarioId);
 * 
 *     @GetMapping("/api/v1/clientes/interno/contexto-ia/{usuarioId}")
 *     ContextoEstrategicoIADTO obtenerContextoIA(@PathVariable UUID usuarioId);
 * }
 * }
 * </pre>
 * 
 * @author Paulo Moron
 * @version 1.1.0
 * @since 2026-05-10
 */
@RestController
@RequestMapping("/api/v1/clientes/interno")
@RequiredArgsConstructor
@Slf4j
public class ControladorInterno {

    private final ServicioContexto servicio;

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
        return ResponseEntity.ok(servicio.obtenerContextoFinanciero(usuarioId));
    }

    /**
     * Retorna el contexto consolidado completo del usuario.
     * Utilizado por ms-nucleo-financiero.
     */
    @GetMapping("/contexto/{usuarioId}")
    public ResponseEntity<com.libreria.comun.dtos.ContextoUsuarioDTO> obtenerContexto(
            @PathVariable UUID usuarioId) {
        log.debug("Solicitud interna de contexto completo para usuarioId={}", usuarioId);
        return ResponseEntity.ok(servicio.obtenerContextoCompleto(usuarioId));
    }
}