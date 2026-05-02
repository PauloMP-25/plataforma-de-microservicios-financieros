package com.nucleo.financiero.infraestructura.clientes;

import com.nucleo.financiero.aplicacion.dtos.ia.RespuestaIaDTO;
import com.nucleo.financiero.aplicacion.dtos.ia.SolicitudIaDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "microservicio-ia", url = "${microservicio.ia.url:http://localhost:8086}")
public interface ClienteIa {

    @PostMapping("/api/v1/ia/analizar")
    RespuestaIaDTO analizarFinanzas(@RequestBody SolicitudIaDTO solicitud);
}
