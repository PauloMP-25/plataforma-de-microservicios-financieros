package com.suscripciones.infraestructura.clientes;

import com.libreria.comun.respuesta.ResultadoApi;
import com.suscripciones.infraestructura.clientes.dtos.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Fallback para el cliente Feign de ms-nucleo-financiero.
 * Se activa si el microservicio destino está offline o si el Circuit Breaker entra en funcionamiento.
 */
@Component
@Slf4j
public class NucleoFinancieroClientFallback implements NucleoFinancieroClient {

    @Override
    public ResultadoApi<List<CategoriaDTO>> listarCategorias(TipoMovimiento tipo) {
        log.error("Fallo al obtener categorías del núcleo financiero (MS offline/timeout).");
        return ResultadoApi.falla(503, "SERVICIO_NO_DISPONIBLE", "El núcleo financiero no está disponible actualmente.", null);
    }

    @Override
    public ResultadoApi<CategoriaDTO> crearCategoria(CategoriaRequestDTO request) {
        log.error("Fallo al crear categoría '{}' en el núcleo financiero (MS offline/timeout).", request.nombre());
        return ResultadoApi.falla(503, "SERVICIO_NO_DISPONIBLE", "El núcleo financiero no está disponible actualmente.", null);
    }

    @Override
    public ResultadoApi<RespuestaTransaccion> registrarTransaccion(SolicitudTransaccion request) {
        log.error("Fallo crítico: No se pudo registrar la transacción de suscripción de monto {} para el usuario {} " +
                "en ms-nucleo-financiero. El circuito de comunicación está degradado.", request.monto(), request.usuarioId());
        return ResultadoApi.falla(503, "SERVICIO_NO_DISPONIBLE", "El núcleo financiero no está disponible actualmente.", null);
    }
}
