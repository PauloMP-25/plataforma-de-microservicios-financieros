package com.nucleo.financiero.presentacion.controladores;

import com.nucleo.financiero.aplicacion.dtos.ia.RespuestaIaDTO;
import com.nucleo.financiero.aplicacion.dtos.ia.SolicitudIaDTO;
import com.nucleo.financiero.aplicacion.servicios.ServicioIa;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ia")
@RequiredArgsConstructor
public class IaController {

    private final ServicioIa servicioIa;

    @PostMapping("/consultar")
    public ResponseEntity<RespuestaIaDTO> consultarIa(
            @Valid @RequestBody SolicitudIaDTO solicitud,
            HttpServletRequest servletRequest) {

        // 1. Extraemos la IP real del cliente desde el request
        String ipCliente = servletRequest.getHeader("X-Forwarded-For");
        if (ipCliente == null) {
            ipCliente = servletRequest.getRemoteAddr();
        }

        // 2. Pasamos tanto la solicitud como la IP al servicio
        // Esto es vital para que la auditoría en RabbitMQ no falle
        return ResponseEntity.ok(servicioIa.obtenerConsejoIA(solicitud, ipCliente));
    }
}
