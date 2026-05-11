package com.auditoria.aplicacion.servicios;

import com.auditoria.aplicacion.dtos.*;
import com.auditoria.dominio.entidades.*;
import com.auditoria.dominio.entidades.AuditoriaAcceso.EstadoAcceso;
import com.auditoria.dominio.repositorios.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Servicio central de seguridad en auditoría.
 *
 * Responsabilidades: 1. Registrar intentos de acceso (éxito/fallo). 2. Detectar
 * fuerza bruta: ≥3 fallos en 10 min → bloquear IP 30 min. 3. Registrar cambios
 * transaccionales para trazabilidad. 4. Verificar si una IP está activamente
 * bloqueada.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ServicioSeguridadAuditoria {

    private final AuditoriaAccesoRepository repositorioAcceso;
    private final AuditoriaTransaccionalRepository repositorioTransaccional;
    private final ListaNegraIpRepository repositorioListaNegra;

    // ── Configuración ─────────────────────────────────────────────────────────
    @Value("${auditoria.seguridad.max-intentos-fallidos:3}")
    private int maxIntentosFallidos;

    @Value("${auditoria.seguridad.ventana-minutos:10}")
    private long ventanaMinutos;

    @Value("${auditoria.seguridad.bloqueo-minutos:30}")
    private long bloqueoMinutos;

    // =========================================================================
    // 1. REGISTRO DE ACCESOS
    // =========================================================================
    /**
     * Persiste el evento de acceso y, si es un FALLO, evalúa si la IP debe ser
     * bloqueada automáticamente por exceder el umbral de intentos.
     *
     * @param dto Datos del intento de acceso
     * @return El registro creado
     */
    @Transactional
    public AuditoriaAccesoDTO registrarAcceso(AuditoriaAccesoRequestDTO dto) {
        log.info("[LOG-SEGURIDAD] {} | Usuario: {} | IP: {}",
                dto.estado(), dto.usuarioId(), dto.ipOrigen());

        AuditoriaAcceso entidad = AuditoriaAcceso.builder()
                .usuarioId(dto.usuarioId())
                .ipOrigen(dto.ipOrigen())
                .navegador(dto.navegador())
                .estado(dto.estado())
                .detalleError(dto.detalleError())
                .fecha(dto.fecha() != null ? dto.fecha() : LocalDateTime.now())
                .build();

        AuditoriaAcceso guardado = repositorioAcceso.save(entidad);

        // Verificar y actuar si fue un fallo
        if (dto.estado() == EstadoAcceso.FALLO) {
            verificarIntentoFallido(dto.ipOrigen());
        }

        return convertirAccesoADTO(guardado);
    }

    // =========================================================================
    // 2. LÓGICA DE DETECCIÓN DE FUERZA BRUTA
    // =========================================================================
    /**
     * Cuenta los fallos recientes de la IP y la bloquea si supera el umbral.
     *
     * Algoritmo: - Ventana deslizante de {@code ventanaMinutos} minutos - Si
     * fallos >= {@code maxIntentosFallidos} → registrar en lista negra por
     * {@code bloqueoMinutos} minutos
     *
     * @param ipOrigen Dirección IP del intento fallido
     */
    @Transactional
    public void verificarIntentoFallido(String ipOrigen) {
        LocalDateTime ventanaDesde = LocalDateTime.now().minusMinutes(ventanaMinutos);

        long fallosRecientes = repositorioAcceso.contarIntentosPorIpYEstadoDesde(
                ipOrigen, EstadoAcceso.FALLO, ventanaDesde
        );

        log.debug("[FUERZA-BRUTA] IP={}, fallos en ventana={}/{}", ipOrigen, fallosRecientes, maxIntentosFallidos);

        if (fallosRecientes >= maxIntentosFallidos) {
            bloquearIpAutomaticamente(ipOrigen, fallosRecientes);
        }
    }

    /**
     * Registra o extiende el bloqueo de una IP en la lista negra.
     */
    private void bloquearIpAutomaticamente(String ip, long cantidadFallos) {
        LocalDateTime ahora = LocalDateTime.now();

        // Si ya existe, extendemos el bloqueo; si no, creamos el registro
        ListaNegraIp registro = repositorioListaNegra.findById(ip)
                .orElseGet(() -> ListaNegraIp.builder()
                .ip(ip)
                .fechaBloqueo(ahora)
                .build());

        registro.setMotivo(String.format(
                "Bloqueo automático: %d intentos fallidos en %d minutos.",
                cantidadFallos, ventanaMinutos
        ));
        registro.setFechaExpiracion(ahora.plusMinutes(bloqueoMinutos));

        repositorioListaNegra.save(registro);

        log.warn("[LISTA-NEGRA] IP bloqueada automáticamente: ip={}, fallos={}, expira={}",
                ip, cantidadFallos, registro.getFechaExpiracion());
    }

    // =========================================================================
    // 3. REGISTRO DE CAMBIOS TRANSACCIONALES
    // =========================================================================
    /**
     * Persiste la trazabilidad de un cambio en una entidad de negocio. Captura
     * el estado anterior y posterior para auditorías de cumplimiento.
     *
     * @param dto Datos del cambio con valorAnterior/valorNuevo en JSON
     * @return El registro de trazabilidad creado
     */
    @Transactional
    public AuditoriaTransaccionalDTO registrarCambio(AuditoriaTransaccionalRequestDTO dto) {
        log.info("[AUDITORIA-TRANSAC] Registrando cambio: entidad={}, id={}, servicio={}",
                dto.entidadAfectada(), dto.entidadId(), dto.servicioOrigen());

        AuditoriaTransaccional entidad = AuditoriaTransaccional.builder()
                .usuarioId(dto.usuarioId())
                .servicioOrigen(dto.servicioOrigen())
                .entidadAfectada(dto.entidadAfectada())
                .entidadId(dto.entidadId())
                .valorAnterior(dto.valorAnterior())
                .valorNuevo(dto.valorNuevo())
                .fecha(dto.fecha())
                .build();

        return convertirTransaccionalADTO(repositorioTransaccional.save(entidad));
    }

    // =========================================================================
    // 4. VERIFICACIÓN DE IP EN LISTA NEGRA
    // =========================================================================
    /**
     * Comprueba si una IP está activamente bloqueada en este momento. Endpoint
     * principal consultado por el API Gateway antes de enrutar peticiones.
     *
     * @param ip Dirección IP a verificar
     * @return DTO con resultado y detalle del bloqueo si aplica
     */
    @Transactional(readOnly = true)
    public RespuestaVerificacionIpDTO verificarIp(String ip) {
        return repositorioListaNegra
                .findActivaByIp(ip, LocalDateTime.now())
                .map(registro -> {
                    log.debug("[VERIFICAR-IP] IP bloqueada encontrada: ip={}, expira={}", ip, registro.getFechaExpiracion());
                    return RespuestaVerificacionIpDTO.bloqueada(
                            ip,
                            registro.getMotivo(),
                            registro.getFechaExpiracion()
                    );
                })
                .orElseGet(() -> {
                    log.debug("[VERIFICAR-IP] IP libre: {}", ip);
                    return RespuestaVerificacionIpDTO.libre(ip);
                });
    }

    // =========================================================================
    // 5. CONSULTAS
    // =========================================================================
    @Transactional(readOnly = true)
    public Page<AuditoriaAccesoDTO> listarAccesos(Pageable paginacion) {
        return repositorioAcceso.findAll(paginacion).map(this::convertirAccesoADTO);
    }

    @Transactional(readOnly = true)
    public Page<AuditoriaTransaccionalDTO> listarCambiosTransaccionales(
            String servicioOrigen, LocalDateTime desde, LocalDateTime hasta, Pageable paginacion) {
        String servicioFiltro = (servicioOrigen != null && servicioOrigen.isBlank()) ? null : servicioOrigen;
        return repositorioTransaccional
                .buscarConFiltros(servicioFiltro, desde, hasta, paginacion)
                .map(this::convertirTransaccionalADTO);
    }

    // =========================================================================
    // 6. MANTENIMIENTO PROGRAMADO
    // =========================================================================
    /**
     * Cada hora: limpia bloqueos de IP expirados de la lista negra.
     */
    @Scheduled(fixedRate = 10000)
    @Transactional
    public void limpiarBloqueoExpirados() {
        int eliminados = repositorioListaNegra.eliminarBloqueoExpirados(LocalDateTime.now());
        if (eliminados > 0) {
            log.info("[MANTENIMIENTO] {} bloqueos de IP expirados eliminados.", eliminados);
        }
    }

    /**
     * Cada 24 horas: purga registros de acceso con más de 90 días de
     * antigüedad.
     */
    @Scheduled(fixedRate = 86_400_000)
    @Transactional
    public void limpiarAccesosAntiguos() {
        LocalDateTime umbral = LocalDateTime.now().minusDays(90);
        int eliminados = repositorioAcceso.eliminarRegistrosAnterioresA(umbral);
        if (eliminados > 0) {
            log.info("[MANTENIMIENTO] {} registros de acceso antiguos eliminados.", eliminados);
        }
    }

    // =========================================================================
    // MAPPERS PRIVADOS
    // =========================================================================
    private AuditoriaAccesoDTO convertirAccesoADTO(AuditoriaAcceso e) {
        return new AuditoriaAccesoDTO(
                e.getId(), e.getUsuarioId(), e.getIpOrigen(),
                e.getNavegador(), e.getEstado(), e.getDetalleError(), e.getFecha()
        );
    }

    private AuditoriaTransaccionalDTO convertirTransaccionalADTO(AuditoriaTransaccional e) {
        return new AuditoriaTransaccionalDTO(
                e.getId(), e.getUsuarioId(), e.getServicioOrigen(),
                e.getEntidadAfectada(), e.getEntidadId(),
                e.getValorAnterior(), e.getValorNuevo(), e.getFecha()
        );
    }
}
