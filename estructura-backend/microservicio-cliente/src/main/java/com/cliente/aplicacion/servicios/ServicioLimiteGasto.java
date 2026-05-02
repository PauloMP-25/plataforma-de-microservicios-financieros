package com.cliente.aplicacion.servicios;

import com.cliente.aplicacion.dtos.*;
import com.cliente.aplicacion.excepciones.*;
import com.cliente.dominio.entidades.LimiteGasto;
import com.cliente.dominio.repositorios.LimiteGastoRepositorio;
import com.cliente.infraestructura.mensajeria.PublicadorAuditoria;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Lógica de negocio para la gestión de límites de gasto por categoría.
 *
 * Publica eventos a RabbitMQ: - LIMITE_CREADO → cuando se crea un nuevo límite
 * - LIMITE_ALCANZADO → cuando el gasto reportado supera el umbral configurado
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ServicioLimiteGasto {

    private final LimiteGastoRepositorio repositorio;
    private final PublicadorAuditoria publicadorAuditoria;

    /**
     * Crea un nuevo límite de gasto para una categoría. Falla con 409 si ya
     * existe un límite para la misma categoría.
     *
     * @param usuarioIdToken
     * @param solicitud
     * @param ipOrigen
     * @return
     */
    @Transactional
    public RespuestaLimiteGasto crear(UUID usuarioIdToken,
            SolicitudLimiteGasto solicitud,
            String ipOrigen) {

        repositorio.findByUsuarioIdAndActivoTrue(usuarioIdToken).ifPresent((LimiteGasto limite) -> {
            if (!limite.estaVencido()) {
                throw new LimiteGastoException("Ya tienes un límite global activo y vigente.");
            }
            // Si está vencido, lo desactivamos para permitir el nuevo
            limite.setActivo(false);
            repositorio.save(limite);
        });

        LimiteGasto nuevo = LimiteGasto.builder()
                .usuarioId(usuarioIdToken)
                .montoLimite(solicitud.getMontoLimite())
                .porcentajeAlerta(solicitud.getPorcentajeAlerta() != null ? solicitud.getPorcentajeAlerta() : 80)
                .fechaInicio(LocalDateTime.now())
                .fechaFin(LocalDateTime.now().plusMonths(1))
                .activo(true)
                .build();

        LimiteGasto guardado = repositorio.save(nuevo);

        publicadorAuditoria.publicar(EventoAuditoria.de(usuarioIdToken.toString(),
                "LIMITE_GLOBAL_CREADO", ipOrigen,
                String.format("Límite global: S/ %.2f hasta %s",
                        guardado.getMontoLimite(), guardado.getFechaFin())));

        return convertirADTO(guardado);
    }

    /**
     * Actualiza un límite de gasto existente por categoría.
     */
    /**
     * Actualiza el límite global ACTIVO.
     *
     * @param usuarioId
     * @param solicitud
     * @param ip
     * @return
     */
    @Transactional
    public RespuestaLimiteGasto actualizar(UUID usuarioId, SolicitudLimiteGasto solicitud, String ip) {
        LimiteGasto limite = repositorio.findByUsuarioIdAndActivoTrue(usuarioId)
                .orElseThrow(() -> new LimiteGastoNoEncontradoException(usuarioId));

        if (limite.estaVencido()) {
            throw new IllegalStateException("El límite actual ha vencido y no se puede modificar. Crea uno nuevo.");
        }

        if (solicitud.getMontoLimite() != null) {
            limite.setMontoLimite(solicitud.getMontoLimite());
        }
        if (solicitud.getPorcentajeAlerta() != null) {
            limite.setPorcentajeAlerta(solicitud.getPorcentajeAlerta());
        }

        LimiteGasto actualizado = repositorio.save(limite);

        publicadorAuditoria.publicar(EventoAuditoria.de(usuarioId.toString(), "LIMITE_GLOBAL_ACTUALIZADO", ip,
                "Se modificaron los parámetros del límite global activo"));

        return convertirADTO(actualizado);
    }

    /**
     * Lista todos los límites del usuario, ordenados alfabéticamente
     *
     * @param usuarioId
     * @return
     */
    @Transactional(readOnly = true)
    public List<RespuestaLimiteGasto> listarHistorial(UUID usuarioId) {
        // Asumiendo que el repo tiene un método findByUsuarioIdOrderByFechaCreacionDesc
        return repositorio.findByUsuarioIdOrderByFechaCreacionDesc(usuarioId)
                .stream()
                .map(this::convertirADTO)
                .collect(Collectors.toList());
    }
    

    @Transactional(readOnly = true)
    public RespuestaLimiteGasto obtenerActivo(UUID usuarioId) {
        return repositorio.findByUsuarioIdAndActivoTrue(usuarioId)
                .map(this::convertirADTO)
                .orElseThrow(() -> new LimiteGastoNoEncontradoException(usuarioId));
    }

    /**
     * Desactiva (eliminación lógica) el límite global actual.
     * @param usuarioId
     * @param ip
     */
    @Transactional
    public void eliminar(UUID usuarioId, String ip) {
        LimiteGasto limite = repositorio.findByUsuarioIdAndActivoTrue(usuarioId)
                .orElseThrow(() -> new DatosPersonalesNoEncontradosException(usuarioId));

        limite.setActivo(false);
        repositorio.save(limite);

        publicadorAuditoria.publicar(EventoAuditoria.de(usuarioId.toString(), "LIMITE_GLOBAL_ELIMINADO", ip,
                "El usuario ha desactivado su límite global actual"));

        log.info("Límite global desactivado para usuario: {}", usuarioId);
    }

    /**
     * Evalúa el gasto TOTAL del usuario contra su límite global único.
     *
     * @param usuarioId
     * @param gastoTotalActual
     * @param ipOrigen
     * @return
     */
    @Transactional(readOnly = true)
    public boolean evaluarYNotificarLimiteGlobal(UUID usuarioId, BigDecimal gastoTotalActual, String ipOrigen) {
        return repositorio.findByUsuarioIdAndActivoTrue(usuarioId)
                .map(limite -> {
                    if (!limite.estaVencido() && limite.haAlcanzadoUmbral(gastoTotalActual)) {
                        publicadorAuditoria.publicar(EventoAuditoria.de(
                                "sistema", "ALERTA_PRESUPUESTO_GLOBAL", ipOrigen,
                                String.format("Gasto total S/ %.2f alcanzó el %d%% de tu presupuesto global S/ %.2f",
                                        gastoTotalActual, limite.getPorcentajeAlerta(), limite.getMontoLimite())
                        ));
                        return true;
                    }
                    return false;
                })
                .orElse(false);
    }

    // =========================================================================
    // Soporte interno
    // =========================================================================
    public RespuestaLimiteGasto convertirADTO(LimiteGasto e) {
        return new RespuestaLimiteGasto(
                e.getId(),
                e.getUsuarioId(),
                e.getMontoLimite(),
                e.getPorcentajeAlerta(),
                e.getFechaInicio(), // Ahora usamos fechas de periodo
                e.getFechaFin(),
                e.isActivo(),
                e.getFechaCreacion()
        );
    }
}
