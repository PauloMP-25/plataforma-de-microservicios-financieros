package com.auditoria.presentacion.controladores;

import com.auditoria.aplicacion.dtos.RegistroAuditoriaDTO;
import com.auditoria.aplicacion.dtos.RegistroAuditoriaRequestDTO;
import com.auditoria.aplicacion.servicios.ServicioRegistroAuditoria;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador para la persistencia y consulta de trazas de auditoría.
 * Centraliza los eventos de todos los microservicios del ecosistema.
 * * @author Paulo
 */
@RestController
@RequestMapping("/api/v1/auditoria")
@RequiredArgsConstructor
public class RegistroAuditoriaController {

    private final ServicioRegistroAuditoria servicioAuditoria;

/**
     * Registra un nuevo evento de auditoría.
     * Invocado usualmente por otros microservicios (vía Feign o mensajería).
     * * @param request Datos del evento (usuario, acción, módulo, etc.)
     * @return Registro guardado con su ID y timestamp generado.
     */
    @PostMapping("/registrar")
    public ResponseEntity<RegistroAuditoriaDTO> registrarEvento(
            @Valid @RequestBody RegistroAuditoriaRequestDTO request) {

        RegistroAuditoriaDTO creado = servicioAuditoria.registrarEvento(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

/**
     * Consulta histórica de registros con soporte para paginación y filtros.
     * * @param modulo (Opcional) Filtrar por microservicio (ej. "USUARIO-SERVICE")
     * @param nivel  (Opcional) Filtrar por severidad (INFO, WARNING, ERROR)
     * @param pagina Número de página (0 por defecto)
     * @param tamanio Cantidad de registros por página (máx. 100)
     */
    @GetMapping("/registros")
    public ResponseEntity<Page<RegistroAuditoriaDTO>> listarRegistros(
            @RequestParam(required = false) String modulo,
            @RequestParam(required = false) String nivel,
            @RequestParam(defaultValue = "0")  int pagina,
            @RequestParam(defaultValue = "20") int tamanio) {

        // Tamaño máximo de página para evitar abusos
        int paginaSegura = Math.max(0, pagina);
        int tamanioSeguro = Math.min(tamanio, 100);

        Pageable paginacion = PageRequest.of(paginaSegura, tamanioSeguro);
        Page<RegistroAuditoriaDTO> resultado =
                servicioAuditoria.listarRegistros(modulo, paginacion);

        return ResponseEntity.ok(resultado);
    }
}
