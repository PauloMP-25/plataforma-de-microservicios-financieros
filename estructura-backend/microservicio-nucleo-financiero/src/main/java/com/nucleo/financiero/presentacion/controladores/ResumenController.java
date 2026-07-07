package com.nucleo.financiero.presentacion.controladores;

import com.libreria.comun.excepciones.ExcepcionAccesoDenegado;
import com.libreria.comun.respuesta.ResultadoApi;
import com.libreria.comun.utilidades.UtilidadSeguridad;
import com.nucleo.financiero.aplicacion.dtos.RachaDTO;
import com.nucleo.financiero.aplicacion.puertos.IResumenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Controlador REST para consultas de resúmenes financieros y métricas de gamificación.
 */
@RestController
@RequestMapping("/api/v1/resumen")
@RequiredArgsConstructor
@Slf4j
@Validated
public class ResumenController {

    private final IResumenService resumenService;

    /**
     * Calcula la racha de días consecutivos (o válidos) en los que el usuario
     * ha registrado transacciones.
     *
     * @param usuarioId ID del usuario a consultar
     * @return RachaDTO con los días de racha actuales y días activos del mes
     */
    @GetMapping("/racha")
    public ResponseEntity<ResultadoApi<RachaDTO>> obtenerRacha(
            @RequestParam(required = false) UUID usuarioId) {

        UUID tokenUsuarioId = UtilidadSeguridad.obtenerUsuarioId();
        
        // Si no mandan usuarioId explícito, usamos el del token
        UUID targetUsuarioId = (usuarioId != null) ? usuarioId : tokenUsuarioId;
        
        if (targetUsuarioId == null) {
            throw new IllegalArgumentException("Debe proveer un usuarioId o enviar un token válido");
        }

        // Si mandan usuarioId pero no coincide con el token (y hay token)
        if (tokenUsuarioId != null && !tokenUsuarioId.equals(targetUsuarioId)) {
            throw new ExcepcionAccesoDenegado();
        }

        RachaDTO racha = resumenService.calcularRacha(targetUsuarioId);
        return ResponseEntity.ok(ResultadoApi.exito(racha, "Racha calculada exitosamente"));
    }
}
