package com.cliente.presentacion.controladores;

import com.cliente.aplicacion.dtos.RespuestaContextoCliente;
import com.cliente.aplicacion.servicios.ServicioContextoCliente;
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
 *   GET /api/v1/clientes/interno/contexto/{usuarioId}
 *
 * Interfaz Feign sugerida en microservicio-nucleo-financiero:
 * <pre>
 * {@code
 * @FeignClient(name = "microservicio-cliente")
 * public interface ClienteContextoFeign {
 *
 *     @GetMapping("/api/v1/clientes/interno/contexto/{usuarioId}")
 *     RespuestaContextoCliente obtenerContexto(@PathVariable UUID usuarioId);
 * }
 * }
 * </pre>
 */
@RestController
@RequestMapping("/api/v1/clientes/interno")
@RequiredArgsConstructor
@Slf4j
public class ControladorInterno {

    private final ServicioContextoCliente servicio;

    /**
     * Retorna el contexto completo del cliente (datos personales + perfil
     * financiero + metas activas + límites de gasto).
     * @param usuarioId
     * @return 
     */
    @GetMapping("/contexto/{usuarioId}")
    public ResponseEntity<RespuestaContextoCliente> obtenerContexto(
            @PathVariable UUID usuarioId) {
        log.debug("Solicitud interna de contexto para usuarioId={}", usuarioId);
        return ResponseEntity.ok(servicio.obtenerContexto(usuarioId));
    }
}