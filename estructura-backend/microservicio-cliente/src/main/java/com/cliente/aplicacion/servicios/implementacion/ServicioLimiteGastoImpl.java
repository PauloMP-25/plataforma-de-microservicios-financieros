package com.cliente.aplicacion.servicios.implementacion;

import com.cliente.aplicacion.dtos.RespuestaLimiteGasto;
import com.cliente.aplicacion.dtos.SolicitudLimiteGasto;
import com.cliente.aplicacion.excepciones.DatosPersonalesNoEncontradosException;
import com.cliente.aplicacion.excepciones.LimiteGastoException;
import com.cliente.aplicacion.excepciones.LimiteGastoNoEncontradoException;
import com.cliente.aplicacion.servicios.ServicioContexto;
import com.cliente.aplicacion.servicios.ServicioLimiteGasto;
import com.cliente.dominio.entidades.LimiteGasto;
import com.cliente.dominio.repositorios.LimiteGastoRepositorio;
import com.cliente.infraestructura.mensajeria.PublicadorAuditoria;
import com.libreria.comun.dtos.EventoAuditoriaDTO;
import com.libreria.comun.dtos.EventoTransaccionalDTO;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Lógica de negocio para la gestión del limite de gasto global
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
    private final ServicioContexto servicioContexto;

    /**
     * @param usuarioIdToken ID del usuario
     * @param solicitud DTO con datos del límite
     * @param ipOrigen IP del cliente
     * @return RespuestaLimiteGasto con el límite creado
     */
    @SuppressWarnings("null")
    @Override
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
                .fechaInicio(LocalDate.now())
                .fechaFin(LocalDate.now().plusMonths(1))
                .activo(true)
                .build();

        LimiteGasto guardado = repositorio.save(nuevo);
        publicadorAuditoria.publicarEventoExitoso(EventoAuditoriaDTO.crear(
                usuarioIdToken, "LIMITE_GLOBAL_CREADO", "MS-CLIENTE",
                ipOrigen, String.format("Límite global: S/ %.2f hasta %s",
                        guardado.getMontoLimite(), guardado.getFechaFin())));

        servicioContexto.refrescarContextoRedis(usuarioIdToken);
        return convertirADTO(guardado);
    }

    /**
     * @param usuarioId ID del usuario
     * @param solicitud DTO con datos a actualizar
     * @param ip IP del cliente
     * @return RespuestaLimiteGasto con el límite actualizado
     */
    @Override
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
        publicadorAuditoria.publicarTransaccionExitosa(EventoTransaccionalDTO.crear(
                usuarioId, limite.getId(), "MS-CLIENTE", "LIMITE GASTO",
                "Cambiar monto del limite global", limite.getMontoLimite() + "", actualizado.getMontoLimite() + ""));

        servicioContexto.refrescarContextoRedis(usuarioId);
        return convertirADTO(actualizado);
    }

    /**
     * @param usuarioId ID del usuario
     * @return Lista de RespuestaLimiteGasto
     */
    @Override
    @Transactional
    public List<RespuestaLimiteGasto> listarHistorial(UUID usuarioId) {
        // Asumiendo que el repo tiene un método findByUsuarioIdOrderByFechaCreacionDesc
        return repositorio.findByUsuarioIdOrderByFechaCreacionDesc(usuarioId)
                .stream()
                .map(this::convertirADTO)
                .collect(Collectors.toList());
    }

    /**
     * @param usuarioId ID del usuario
     * @return RespuestaLimiteGasto DTO de respuesta
     */
    @Override
    @Transactional
    public RespuestaLimiteGasto obtenerActivo(UUID usuarioId) {
        return repositorio.findByUsuarioIdAndActivoTrue(usuarioId)
                .map(this::convertirADTO)
                .orElseThrow(() -> new LimiteGastoNoEncontradoException(usuarioId));
    }

    /**
     * @param usuarioId ID del usuario
     * @param ip IP del cliente
     */
    @Override
    @Transactional
    public void eliminar(UUID usuarioId, String ip) {
        LimiteGasto limite = repositorio.findByUsuarioIdAndActivoTrue(usuarioId)
                .orElseThrow(() -> new DatosPersonalesNoEncontradosException(usuarioId));

        limite.setActivo(false);
        repositorio.save(limite);

        publicadorAuditoria.publicarTransaccionExitosa(EventoTransaccionalDTO.crear(
                usuarioId, limite.getId(), "MS-CLIENTE", "LIMITE GLOBAL",
                "Eliminando limite global", "ACTIVO", "DESACTIVADO"));
        log.info("Límite global desactivado para usuario: {}", usuarioId);
        servicioContexto.refrescarContextoRedis(usuarioId);
    }

    /**
     * @param usuarioId ID del usuario
     * @param gastoTotalActual Gasto total actual
     * @param ipOrigen IP del cliente
     * @return true si se notificó el límite, false en caso contrario
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
                    }
                    return false;
                })
                .orElse(false);
    }

    @Override
    public RespuestaLimiteGasto convertirADTO(LimiteGasto limite) {
        return new RespuestaLimiteGasto(
                limite.getId(),
                limite.getUsuarioId(),
                limite.getMontoLimite(),
                limite.getPorcentajeAlerta(),
                limite.getFechaInicio(),
                limite.getFechaFin(),
                limite.isActivo(),
                limite.getFechaCreacion());
    }
}
