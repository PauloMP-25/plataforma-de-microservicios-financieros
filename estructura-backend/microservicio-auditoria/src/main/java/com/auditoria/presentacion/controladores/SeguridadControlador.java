package com.auditoria.presentacion.controladores;

import com.auditoria.aplicacion.dtos.RespuestaVerificacionIpDTO;
import com.auditoria.aplicacion.servicios.ServicioSeguridadAuditoria;
import com.libreria.comun.respuesta.ResultadoApi;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador REST que expone las operaciones de seguridad y defensa
 * perimetral.
 * <p>
 * Proporciona endpoints críticos para la evaluación de amenazas en tiempo real.
 * Principalmente utilizado por el API Gateway para consultar si una IP origen
 * se encuentra en la lista negra antes de permitir el enrutamiento de
 * peticiones.
 * </p>
 * 
 * @author Paulo Moron
 * @version 1.1.0
 * @since 2026-05-10
 */
@RestController
@RequestMapping("/api/v1/seguridad")
@RequiredArgsConstructor
public class SeguridadControlador {

    private final ServicioSeguridadAuditoria servicioSeguridad;

    /**
     * Verifica el estado actual de una dirección IP frente a la lista negra.
     * <p>
     * Este endpoint es consultado de manera intensiva por el Gateway. Retorna un
     * DTO que indica si la IP está libre o si tiene un bloqueo activo (y hasta
     * cuándo).
     * </p>
     * 
     * @param ip Dirección IP a consultar.
     * @return {@link ResponseEntity} conteniendo el {@link ResultadoApi} con los
     *         datos de verificación.
     */
    @GetMapping("/verificar-ip/{ip}")
    public ResponseEntity<ResultadoApi<RespuestaVerificacionIpDTO>> verificarIp(@PathVariable String ip) {
        RespuestaVerificacionIpDTO respuesta = servicioSeguridad.verificarEstadoIp(ip);

        return ResponseEntity.ok(
                ResultadoApi.exito(
                        respuesta,
                        "Verificación de estado de IP completada.", null));
    }
}
