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
        if (repositorio.existsByUsuarioIdAndCategoriaId(usuarioIdToken, solicitud.getCategoriaId())) {
            throw new LimiteGastoDuplicadoException(solicitud.getCategoriaId());
        }

        LimiteGasto limite = LimiteGasto.builder()
                .usuarioId(usuarioIdToken)
                .categoriaId(solicitud.getCategoriaId())
                .montoLimite(solicitud.getMontoLimite())
                .porcentajeAlerta(solicitud.getPorcentajeAlerta() != null
                        ? solicitud.getPorcentajeAlerta()
                        : 80)
                .build();

        LimiteGasto guardado = repositorio.save(limite);

        publicadorAuditoria.publicar(EventoAuditoria.de(
                usuarioIdToken.toString(), "LIMITE_CREADO", ipOrigen,
                String.format("Límite creado: categoría='%s' monto=S/ %.2f alerta=%d%%",
                        guardado.getCategoriaId(), guardado.getMontoLimite(),
                        guardado.getPorcentajeAlerta())
        ));

        return convertirADTO(guardado);
    }

    /**
     * Actualiza un límite de gasto existente por categoría.
     *
     * @param usuarioIdToken
     * @param categoriaId
     * @param solicitud
     * @param ipOrigen
     * @return
     */
    @Transactional
    public RespuestaLimiteGasto actualizar(UUID usuarioIdToken, String categoriaId,
            SolicitudLimiteGasto solicitud, String ipOrigen) {
        LimiteGasto limite = repositorio.findByUsuarioIdAndCategoriaId(usuarioIdToken, categoriaId)
                .orElseThrow(() -> new DatosPersonalesNoEncontradosException(usuarioIdToken));

        if (solicitud.getMontoLimite() != null) {
            limite.setMontoLimite(solicitud.getMontoLimite());
        }
        if (solicitud.getPorcentajeAlerta() != null) {
            limite.setPorcentajeAlerta(solicitud.getPorcentajeAlerta());
        }

        return convertirADTO(repositorio.save(limite));
    }

    /**
     * Lista todos los límites del usuario, ordenados alfabéticamente por
     * categoría.
     *
     * @param usuarioIdToken
     * @return
     */
    @Transactional(readOnly = true)
    public List<RespuestaLimiteGasto> listar(UUID usuarioIdToken) {
        return repositorio.findByUsuarioIdOrderByCategoriaIdAsc(usuarioIdToken)
                .stream()
                .map(this::convertirADTO)
                .collect(Collectors.toList());
    }

    /**
     * Elimina el límite de una categoría específica del usuario.
     *
     * @param usuarioIdToken
     * @param categoriaId
     * @param ipOrigen
     */
    @Transactional
    public void eliminar(UUID usuarioIdToken, String categoriaId, String ipOrigen) {
        if (!repositorio.existsByUsuarioIdAndCategoriaId(usuarioIdToken, categoriaId)) {
            throw new DatosPersonalesNoEncontradosException(usuarioIdToken);
        }
        repositorio.deleteByUsuarioIdAndCategoriaId(usuarioIdToken, categoriaId);
        log.info("Límite eliminado: usuario={} categoría='{}'", usuarioIdToken, categoriaId);

        publicadorAuditoria.publicar(EventoAuditoria.de(
                usuarioIdToken.toString(), "LIMITE_ELIMINADO", ipOrigen,
                "Límite eliminado para categoría: " + categoriaId
        ));
    }

    /**
     * Evalúa si el gasto actual de una categoría supera el umbral de alerta. Si
     * se supera, publica el evento LIMITE_ALCANZADO. Llamado típicamente por el
     * microservicio-nucleo-financiero.
     *
     * @param usuarioId usuario propietario del límite
     * @param categoriaId nombre de la categoría (texto libre)
     * @param gastoActual monto gastado en el período actual
     * @param ipOrigen
     * @return true si se alcanzó el umbral
     */
    @Transactional(readOnly = true)
    public boolean evaluarYNotificarLimite(UUID usuarioId, String categoriaId,
            BigDecimal gastoActual, String ipOrigen) {
        return repositorio.findByUsuarioIdAndCategoriaId(usuarioId, categoriaId)
                .map((LimiteGasto limite) -> {
                    if (limite.haAlcanzadoUmbral(gastoActual)) {
                        publicadorAuditoria.publicar(EventoAuditoria.de(
                                "sistema", "LIMITE_ALCANZADO", ipOrigen,
                                String.format("Categoría '%s': gasto S/ %.2f alcanzó el %d%% del límite S/ %.2f",
                                        categoriaId, gastoActual, limite.getPorcentajeAlerta(),
                                        limite.getMontoLimite())
                        ));
                        log.warn("LIMITE ALCANZADO: usuario={} categoría='{}' gasto={}",
                                usuarioId, categoriaId, gastoActual);
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
                e.getId(), e.getUsuarioId(), e.getCategoriaId(),
                e.getMontoLimite(), e.getPorcentajeAlerta(),
                e.getFechaCreacion(), e.getFechaActualizacion()
        );
    }
}
