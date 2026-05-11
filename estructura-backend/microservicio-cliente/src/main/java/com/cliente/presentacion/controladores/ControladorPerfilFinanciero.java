package com.cliente.presentacion.controladores;

import com.cliente.aplicacion.dtos.RespuestaPerfilFinanciero;
import com.cliente.aplicacion.dtos.SolicitudPerfilFinanciero;
import com.cliente.aplicacion.servicios.ServicioPerfilFinanciero;
import com.libreria.comun.utilidades.UtilidadIp;
import com.libreria.comun.utilidades.UtilidadSeguridad;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import com.libreria.comun.respuesta.ResultadoApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Controlador para el perfil financiero del cliente.
 *
 * Rutas:
 * PUT /api/v1/clientes/perfil-financiero/{usuarioId} → crear o actualizar
 * perfil financiero
 * GET /api/v1/clientes/perfil-financiero/{usuarioId} → consultar perfil
 * financiero
 */
@RestController
@RequestMapping("/api/v1/clientes/perfil-financiero")
@RequiredArgsConstructor
@Slf4j
public class ControladorPerfilFinanciero {

    private final ServicioPerfilFinanciero servicio;

    /**
     * Crea o actualiza el perfil financiero del usuario autenticado (upsert).
     * 
     * @param usuarioId Identificador del usuario en la ruta
     * @param solicitud DTO con los datos financieros a actualizar
     * @param request   Petición HTTP para extraer la IP
     * @return ResultadoApi con el perfil financiero actualizado
     */
    @PutMapping("/{usuarioId}")
    public ResponseEntity<ResultadoApi<RespuestaPerfilFinanciero>> guardarOActualizar(
            @PathVariable UUID usuarioId,
            @Valid @RequestBody SolicitudPerfilFinanciero solicitud,
            HttpServletRequest request) {

        UUID usuarioToken = UtilidadSeguridad.obtenerUsuarioId();
        String ip = UtilidadIp.obtenerIpReal(request);
        RespuestaPerfilFinanciero respuesta = servicio.guardarOActualizar(usuarioId, usuarioToken, solicitud, ip);
        return ResponseEntity
                .ok(ResultadoApi.exito(respuesta, "Perfil financiero guardado/actualizado con éxito.", null));
    }

    /**
     * Consulta el perfil financiero del usuario autenticado.
     * 
     * @param usuarioId Identificador del usuario en la ruta
     * @param request   Petición HTTP
     * @return ResultadoApi con los datos del perfil financiero
     */
    @GetMapping("/{usuarioId}")
    public ResponseEntity<ResultadoApi<RespuestaPerfilFinanciero>> consultar(
            @PathVariable UUID usuarioId,
            HttpServletRequest request) {

        UUID usuarioID = UtilidadSeguridad.obtenerUsuarioId();
        RespuestaPerfilFinanciero respuesta = servicio.consultar(usuarioId, usuarioID);
        return ResponseEntity.ok(ResultadoApi.exito(respuesta, "Perfil financiero recuperado.", null));
    }
}