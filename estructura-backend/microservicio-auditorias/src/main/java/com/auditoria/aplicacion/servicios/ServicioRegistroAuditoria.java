package com.auditoria.aplicacion.servicios;

import com.auditoria.aplicacion.dtos.RegistroAuditoriaDTO;
import com.auditoria.aplicacion.dtos.RegistroAuditoriaRequestDTO;
import com.auditoria.dominio.entidades.RegistroAuditoria;
import com.auditoria.dominio.repositorios.RegistroAuditoriaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ServicioRegistroAuditoria {

    private final RegistroAuditoriaRepository repositorioAuditoria;

    // ─── Escritura ────────────────────────────────────────────────────────────

    @Transactional
    public RegistroAuditoriaDTO registrarEvento(RegistroAuditoriaRequestDTO request) {
        log.info("[AUDITORIA] Registrando evento: accion={}, modulo={}",
                 request.accion(), request.modulo());

        RegistroAuditoria entidad = convertirAEntidad(request);
        RegistroAuditoria guardado = repositorioAuditoria.save(entidad);

        log.debug("[AUDITORIA] Registro guardado con id={}", guardado.getId());
        return convertirADTO(guardado);
    }

    // ─── Lectura ──────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<RegistroAuditoriaDTO> listarRegistros(String modulo, Pageable paginacion) {
        log.debug("[AUDITORIA] Consultando registros: modulo={}, nivel={}, pagina={}",
                  modulo, paginacion.getPageNumber());

        // Normalizar cadenas vacías a null para que el query los ignore
        String moduloFiltro = (modulo != null && modulo.isBlank()) ? null : modulo;
        return repositorioAuditoria
                .buscarPorFiltros(moduloFiltro, paginacion)
                .map(this::convertirADTO);
    }

    // ─── Mappers privados ─────────────────────────────────────────────────────

    private RegistroAuditoria convertirAEntidad(RegistroAuditoriaRequestDTO dto) {
        return RegistroAuditoria.builder()
                .fechaHora(dto.fechaHora()) // null → @PrePersist asignará LocalDateTime.now()
                .nombreUsuario(dto.nombreUsuario())
                .accion(dto.accion())
                .modulo(dto.modulo())
                .ipOrigen(dto.ipOrigen())
                .detalles(dto.detalles())
                .build();
    }

    private RegistroAuditoriaDTO convertirADTO(RegistroAuditoria entidad) {
        return new RegistroAuditoriaDTO(
                entidad.getId(),
                entidad.getFechaHora(),
                entidad.getNombreUsuario(),
                entidad.getAccion(),
                entidad.getModulo(),
                entidad.getIpOrigen(),
                entidad.getDetalles()
        );
    }
}
