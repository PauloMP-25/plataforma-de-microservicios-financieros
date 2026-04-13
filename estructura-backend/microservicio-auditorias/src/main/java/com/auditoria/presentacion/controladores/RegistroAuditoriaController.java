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

@RestController
@RequestMapping("/api/v1/auditoria")
@RequiredArgsConstructor
public class RegistroAuditoriaController {

    private final ServicioRegistroAuditoria servicioAuditoria;

    /**
     * POST /api/v1/auditoria/registrar
     * Recibe un evento del microservicio-usuario (u otro microservicio) y lo persiste.
     * @param request
     * @return 
     */
    @PostMapping("/registrar")
    public ResponseEntity<RegistroAuditoriaDTO> registrarEvento(
            @Valid @RequestBody RegistroAuditoriaRequestDTO request) {

        RegistroAuditoriaDTO creado = servicioAuditoria.registrarEvento(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    /**
     * GET /api/auditoria/registros?modulo=IAM-SERVICE&nivel=WARNING&page=0&size=20
     * Devuelve los registros paginados, con filtros opcionales por módulo y nivel.
     * @param modulo
     * @param nivel
     * @param pagina
     * @param tamanio
     * @return 
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
