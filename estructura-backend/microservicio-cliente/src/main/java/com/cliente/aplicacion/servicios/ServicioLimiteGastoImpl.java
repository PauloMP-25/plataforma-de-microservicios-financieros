package com.cliente.aplicacion.servicios;

import com.cliente.aplicacion.dtos.respuestas.RespuestaLimiteGasto;
import com.cliente.aplicacion.dtos.solicitudes.SolicitudLimiteGasto;
import com.cliente.aplicacion.excepciones.LimiteGastoException;
import com.cliente.aplicacion.excepciones.LimiteGastoNoEncontradoException;
import com.cliente.aplicacion.eventos.EventoContextoActualizado;
import com.cliente.aplicacion.puertos.ServicioLimiteGasto;
import com.cliente.dominio.entidades.LimiteGasto;
import com.cliente.dominio.repositorios.LimiteGastoRepositorio;
import com.cliente.infraestructura.mensajeria.PublicadorAuditoria;
import com.libreria.comun.dtos.EventoAuditoriaDTO;
import com.libreria.comun.dtos.EventoTransaccionalDTO;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Lógica de negocio para la gestión del limite de gasto global.
 *
 * @author Paulo Moron
 * @since 2026-05
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ServicioLimiteGastoImpl implements ServicioLimiteGasto {

    private final LimiteGastoRepositorio repositorio;
    private final PublicadorAuditoria publicadorAuditoria;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Crea un nuevo límite de gasto global.
     */
    @Override
    @Transactional
    public RespuestaLimiteGasto crear(UUID usuarioIdToken,
            SolicitudLimiteGasto solicitud,
            String ipOrigen) {

        if (usuarioIdToken == null || solicitud == null) {
            throw new IllegalArgumentException("El ID de usuario y la solicitud no pueden ser nulos.");
        }
        repositorio.findByUsuarioIdAndActivoTrue(usuarioIdToken).ifPresent((LimiteGasto limite) -> {
            if (!limite.estaVencido()) {
                throw new LimiteGastoException("Ya tienes un límite global activo y vigente.");
            }
        });

        // Desactivación eficiente por lote en la base de datos
        repositorio.desactivarLimitesAnteriores(usuarioIdToken);

        LimiteGasto nuevo = LimiteGasto.builder()
                .usuarioId(usuarioIdToken)
                .nombre(solicitud.nombre() != null ? solicitud.nombre() : "Presupuesto Mensual")
                .montoLimite(solicitud.montoLimite())
                .porcentajeAlerta(solicitud.porcentajeAlerta() != null ? solicitud.porcentajeAlerta() : 80)
                .fechaInicio(solicitud.fechaInicio() != null ? solicitud.fechaInicio() : LocalDate.now())
                .fechaFin(solicitud.fechaFin() != null ? solicitud.fechaFin() : LocalDate.now().plusMonths(1))
                .activo(true)
                .build();

        LimiteGasto guardado = repositorio.save(nuevo);
        publicadorAuditoria.publicarEventoExitoso(EventoAuditoriaDTO.crear(
                usuarioIdToken, "LIMITE_GLOBAL_CREADO", "MS-CLIENTE",
                ipOrigen, String.format("Límite global: S/ %.2f hasta %s",
                        guardado.getMontoLimite(), guardado.getFechaFin())));

        eventPublisher.publishEvent(new EventoContextoActualizado(usuarioIdToken, "LIMITE_GLOBAL_CREADO"));
        return convertirADTO(guardado);
    }

    /**
     * Actualiza el límite global ACTIVO.
     */
    @Override
    @Transactional
    public RespuestaLimiteGasto actualizar(UUID usuarioId, SolicitudLimiteGasto solicitud, String ip) {
        LimiteGasto limite = repositorio.findByUsuarioIdAndActivoTrue(usuarioId)
                .orElseThrow(() -> new LimiteGastoNoEncontradoException(usuarioId));

        if (limite.estaVencido()) {
            throw new LimiteGastoException("El límite actual ha vencido y no se puede modificar. Crea uno nuevo.");
        }

        if (solicitud.montoLimite() != null) {
            limite.setMontoLimite(solicitud.montoLimite());
        }
        if (solicitud.porcentajeAlerta() != null) {
            limite.setPorcentajeAlerta(solicitud.porcentajeAlerta());
        }
        if (solicitud.fechaInicio() != null) {
            limite.setFechaInicio(solicitud.fechaInicio());
        }
        if (solicitud.fechaFin() != null) {
            limite.setFechaFin(solicitud.fechaFin());
        }

        // Validar rango de fechas si ambas están definidas
        if (limite.getFechaInicio() != null && limite.getFechaFin() != null && limite.getFechaInicio().isAfter(limite.getFechaFin())) {
            throw new LimiteGastoException("La fecha de inicio debe ser anterior o igual a la fecha de fin.");
        }

        LimiteGasto actualizado = repositorio.save(limite);
        publicadorAuditoria.publicarTransaccionExitosa(EventoTransaccionalDTO.crear(
                usuarioId, limite.getId(), "MS-CLIENTE", "LIMITE GASTO",
                "Cambiar monto del limite global", limite.getMontoLimite() + "", actualizado.getMontoLimite() + ""));

        eventPublisher.publishEvent(new EventoContextoActualizado(usuarioId, "LIMITE_GLOBAL_ACTUALIZADO"));
        return convertirADTO(actualizado);
    }

    /**
     * Lista todos los límites del usuario.
     */
    @Override
    @Transactional(readOnly = true)
    public List<RespuestaLimiteGasto> listarHistorial(UUID usuarioId) {
        return repositorio.findByUsuarioIdOrderByFechaCreacionDesc(usuarioId)
                .stream()
                .map(this::convertirADTO)
                .collect(Collectors.toList());
    }

    /**
     * Obtiene el límite activo del usuario.
     */
    @Override
    @Transactional(readOnly = true)
    public RespuestaLimiteGasto obtenerActivo(UUID usuarioId) {
        return repositorio.findByUsuarioIdAndActivoTrue(usuarioId)
                .map(this::convertirADTO)
                .orElseThrow(() -> new LimiteGastoNoEncontradoException(usuarioId));
    }

    /**
     * Desactiva (eliminación lógica) el límite global actual.
     */
    @Override
    @Transactional
    public void eliminar(UUID usuarioId, String ip) {
        LimiteGasto limite = repositorio.findByUsuarioIdAndActivoTrue(usuarioId)
                .orElseThrow(() -> new LimiteGastoNoEncontradoException(usuarioId));

        limite.setActivo(false);
        repositorio.save(limite);

        publicadorAuditoria.publicarTransaccionExitosa(EventoTransaccionalDTO.crear(
                usuarioId, limite.getId(), "MS-CLIENTE", "LIMITE GLOBAL",
                "Eliminando limite global", "ACTIVO", "DESACTIVADO"));
        log.info("Límite global desactivado para usuario: {}", usuarioId);
        eventPublisher.publishEvent(new EventoContextoActualizado(usuarioId, "LIMITE_GLOBAL_ELIMINADO"));
    }

    /**
     * Evalúa el gasto TOTAL del usuario contra su límite global único.
     */
    @Override
    @Transactional
    public boolean evaluarYNotificarLimiteGlobal(UUID usuarioId, BigDecimal gastoTotalActual, String ipOrigen) {
        return repositorio.findByUsuarioIdAndActivoTrue(usuarioId)
                .map(limite -> {
                    if (!limite.estaVencido() && limite.haAlcanzadoUmbral(gastoTotalActual)) {
                        publicadorAuditoria.publicarEventoExitoso(EventoAuditoriaDTO.crear(
                                usuarioId, "ALERTA_PRESUPUESTO_GLOBAL", "MS-CLIENTE", ipOrigen,
                                String.format("Gasto total S/ %.2f alcanzó el %d%% de tu presupuesto global S/ %.2f",
                                        gastoTotalActual, limite.getPorcentajeAlerta(), limite.getMontoLimite())));
                        return true;
                    }
                    return false;
                })
                .orElse(false);
    }

    /**
     * Consulta interna del límite activo sin validación de JWT (uso para Facade).
     */
    @Override
    @Transactional(readOnly = true)
    public RespuestaLimiteGasto obtenerActivoInterno(UUID usuarioId) {
        return repositorio.findByUsuarioIdAndActivoTrue(usuarioId)
                .map(this::convertirADTO)
                .orElse(null);
    }

    private RespuestaLimiteGasto convertirADTO(LimiteGasto limite) {
        return new RespuestaLimiteGasto(
                limite.getId(),
                limite.getUsuarioId(),
                limite.getNombre(),
                limite.getMontoLimite(),
                limite.getPorcentajeAlerta(),
                limite.getFechaInicio(),
                limite.getFechaFin(),
                limite.isActivo(),
                limite.getFechaCreacion());
    }
}
