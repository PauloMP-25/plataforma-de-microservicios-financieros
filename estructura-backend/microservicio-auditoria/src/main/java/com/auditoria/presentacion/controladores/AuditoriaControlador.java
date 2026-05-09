package com.auditoria.presentacion.controladores;

import com.auditoria.aplicacion.dtos.RespuestaAuditoriaDetalladoDTO;
import com.auditoria.aplicacion.servicios.ServicioRegistroAuditoria;
import com.libreria.comun.dtos.EventoAccesoDTO;
import com.libreria.comun.respuesta.Paginacion;
import com.libreria.comun.respuesta.ResultadoApi;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador de infraestructura para la gestión y consulta de auditoría.
 * <p>
 * Implementa un modelo híbrido: utiliza contratos de la librería común para la
 * ingesta de datos y DTOs locales detallados para la visualización
 * administrativa.
 * </p>
 * 
 * @author Paulo Moron
 * @version 1.2
 * @since 2026-05
 */
@RestController
@RequestMapping("/api/v1/auditoria")
@RequiredArgsConstructor
public class AuditoriaControlador {

        private final ServicioRegistroAuditoria servicioAuditoria;

        /**
         * Registra un nuevo evento de auditoría.
         * <p>
         * Utiliza el {@link EventoAccesoDTO} de la librería común como contrato
         * de entrada para asegurar compatibilidad con otros microservicios.
         * </p>
         * 
         * @param request Datos del evento provenientes de la librería común.
         * @return {@link ResponseEntity} con el resultado de la operación.
         */
        @PostMapping("/registrar")
        public ResponseEntity<ResultadoApi<EventoAccesoDTO>> registrarEvento(
                        @Valid @RequestBody EventoAccesoDTO request) {

                EventoAccesoDTO creado = servicioAuditoria.registrarEvento(request);
                return ResponseEntity.status(201).body(
                                ResultadoApi.creado(creado, "Evento de auditoría registrado correctamente."));
        }

        /**
         * Consulta el histórico detallado de auditoría para el Frontend.
         * <p>
         * Retorna un {@link RespuestaAuditoriaDetalladoDTO} que incluye información
         * contextual (email, nombres) necesaria para la toma de decisiones.
         * </p>
         * 
         * @param modulo  (Opcional) Filtrar por nombre del microservicio.
         * @param pagina  Número de página solicitado.
         * @param tamanio Cantidad de registros por página.
         * @return Respuesta estandarizada con datos detallados y paginación.
         */
        @GetMapping("/registros")
        public ResponseEntity<ResultadoApi<List<RespuestaAuditoriaDetalladoDTO>>> listarRegistros(
                        @RequestParam(required = false) String modulo,
                        @RequestParam(defaultValue = "0") int pagina,
                        @RequestParam(defaultValue = "20") int tamanio) {

                int paginaSegura = Math.max(0, pagina);
                int tamanioSeguro = Math.min(tamanio, 100);

                Pageable paginacionRequest = PageRequest.of(paginaSegura, tamanioSeguro);

                // El servicio ahora debe devolver una página del DTO detallado local
                Page<RespuestaAuditoriaDetalladoDTO> resultadoPage = servicioAuditoria.listarRegistrosDetallados(modulo,
                                paginacionRequest);

                Paginacion<RespuestaAuditoriaDetalladoDTO> infoPaginacion = Paginacion.desde(resultadoPage);

                return ResponseEntity.ok(
                                ResultadoApi.exito(
                                                infoPaginacion.contenido(),
                                                "Consulta de registros detallada realizada con éxito.",
                                                infoPaginacion));
        }
}