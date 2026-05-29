package com.suscripciones.infraestructura.clientes;

import com.libreria.comun.respuesta.ResultadoApi;
import com.suscripciones.infraestructura.clientes.dtos.*;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import org.springframework.web.bind.annotation.RequestHeader;

import java.util.List;

/**
 * Cliente Feign para comunicarse de forma resiliente con el microservicio núcleo financiero.
 */
@FeignClient(
        name = "microservicio-nucleo-financiero",
        url = "${URL_PROD_FINANCIERO:http://localhost:8085}",
        fallback = NucleoFinancieroClientFallback.class
)
public interface NucleoFinancieroClient {

    @GetMapping("/api/v1/financiero/categorias")
    ResultadoApi<List<CategoriaDTO>> listarCategorias(
            @RequestHeader("Authorization") String token,
            @RequestParam(required = false) TipoMovimiento tipo
    );

    @PostMapping("/api/v1/financiero/categorias")
    ResultadoApi<CategoriaDTO> crearCategoria(
            @RequestHeader("Authorization") String token,
            @RequestBody CategoriaRequestDTO request
    );

    @PostMapping("/api/v1/financiero/transacciones")
    ResultadoApi<RespuestaTransaccion> registrarTransaccion(
            @RequestHeader("Authorization") String token,
            @RequestBody SolicitudTransaccion request
    );
}
