package com.pagos.aplicacion.servicios;

import com.libreria.comun.respuesta.Paginacion;
import com.pagos.aplicacion.dtos.ResumenPagosDTO;
import com.pagos.aplicacion.enums.EstadoPago;
import com.pagos.aplicacion.enums.PlanSuscripcion;
import com.pagos.aplicacion.puertos.IServicioAdminPagos;
import com.pagos.dominio.entidades.Pago;
import com.pagos.dominio.repositorios.RepositorioDetallePago;
import com.pagos.dominio.repositorios.RepositorioPago;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Implementación de servicios administrativos para el módulo de pagos.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ServicioAdminPagosImpl implements IServicioAdminPagos {

    private final RepositorioPago repositorioPago;
    private final RepositorioDetallePago repositorioDetallePago;

    @Override
    @Transactional(readOnly = true)
    public ResumenPagosDTO obtenerResumenGeneral() {
        long total = repositorioPago.count();
        BigDecimal ingresos = repositorioDetallePago.sumarIngresosTotales();
        if (ingresos == null) ingresos = BigDecimal.ZERO;

        Map<String, Long> porEstado = new HashMap<>();
        for (EstadoPago estado : EstadoPago.values()) {
            porEstado.put(estado.name(), repositorioPago.countByEstado(estado));
        }

        Map<String, Long> porPlan = new HashMap<>();
        List<Object[]> resultadosPlan = repositorioPago.contarSuscripcionesActivasPorPlan();
        for (Object[] fila : resultadosPlan) {
            porPlan.put(((PlanSuscripcion) fila[0]).name(), (Long) fila[1]);
        }

        return new ResumenPagosDTO(total, ingresos.setScale(2, RoundingMode.HALF_UP), porEstado, porPlan);
    }

    @SuppressWarnings("null")
    @Override
    @Transactional(readOnly = true)
    public Paginacion<Pago> listarTodosLosPagos(int pagina, int tamanio) {
        PageRequest pageRequest = PageRequest.of(pagina, tamanio, Sort.by("fechaCreacion").descending());
        Page<Pago> page = repositorioPago.findAll(pageRequest);
        return Paginacion.desde(page);
    }

    @SuppressWarnings("null")
    @Override
    @Transactional(readOnly = true)
    public Pago buscarPagoPorId(UUID pagoId) {
        return repositorioPago.findById(pagoId)
                .orElseThrow(() -> new IllegalArgumentException("Pago no encontrado: " + pagoId));
    }

    @Override
    @Transactional
    public void actualizarEstadoManual(UUID pagoId, String nuevoEstado) {
        Pago pago = buscarPagoPorId(pagoId);
        try {
            pago.setEstado(EstadoPago.valueOf(nuevoEstado.toUpperCase()));
            repositorioPago.save(pago);
            log.info("[ADMIN] Pago {} actualizado manualmente a {}", pagoId, nuevoEstado);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Estado no válido: " + nuevoEstado);
        }
    }
}
