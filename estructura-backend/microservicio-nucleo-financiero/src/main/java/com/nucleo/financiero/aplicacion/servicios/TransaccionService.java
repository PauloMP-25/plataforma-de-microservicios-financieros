package com.nucleo.financiero.aplicacion.servicios;

import com.nucleo.financiero.aplicacion.dtos.transacciones.TransaccionRequestDTO;
import com.nucleo.financiero.aplicacion.dtos.transacciones.ResumenFinancieroDTO;
import com.nucleo.financiero.aplicacion.dtos.transacciones.TransaccionDTO;
import com.nucleo.financiero.dominio.entidades.Categoria;
import com.nucleo.financiero.dominio.entidades.Categoria.TipoMovimiento;
import com.nucleo.financiero.dominio.entidades.Transaccion;
import com.nucleo.financiero.dominio.repositorios.CategoriaRepository;
import com.nucleo.financiero.dominio.repositorios.TransaccionRepository;
import com.nucleo.financiero.infraestructura.mensajeria.PublicadorAuditoria;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class TransaccionService {

    private final TransaccionRepository transaccionRepository;
    private final CategoriaRepository categoriaRepository;
    private final PublicadorAuditoria publicadorAuditoria;

    @Transactional
    public TransaccionDTO registrar(TransaccionRequestDTO request, String ipCliente) {
        Transaccion guardada = transaccionRepository.save(construirEntidad(request));
        log.info("Transacción registrada: {} — {} {} ({})",
                guardada.getId(), guardada.getTipo(), guardada.getMonto(), guardada.getNombreCliente());

        publicadorAuditoria.publicarRegistro(
                guardada.getUsuarioId(),
                guardada.getId().toString(),
                guardada.getMonto().toString(),
                ipCliente
        );
        return TransaccionDTO.desde(guardada);
    }

    @Transactional
    public List<TransaccionDTO> registrarLote(List<TransaccionRequestDTO> solicitudes, String ipCliente) {
        if (solicitudes == null || solicitudes.isEmpty()) {
            throw new IllegalArgumentException("La lista de transacciones no puede estar vacía.");
        }
        if (solicitudes.size() > 500) {
            throw new IllegalArgumentException(
                    "El lote no puede superar 500 transacciones. Recibidas: " + solicitudes.size());
        }
        log.info("Iniciando registro en lote: {} transacciones", solicitudes.size());
        List<Transaccion> entidades = solicitudes.stream()
                .map(this::construirEntidad)
                .collect(Collectors.toList());
        List<Transaccion> guardadas = transaccionRepository.saveAll(entidades);
        log.info("Lote completado: {} transacciones guardadas", guardadas.size());
        publicadorAuditoria.publicarAcceso(
                guardadas.get(0).getUsuarioId(),
                "REGISTRO_LOTE_TRANSACCIONES",
                "Se registraron " + guardadas.size() + " transacciones exitosamente.",
                ipCliente
        );
        return guardadas.stream().map(TransaccionDTO::desde).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<TransaccionDTO> listarHistorial(
            UUID usuarioId, TipoMovimiento tipo, UUID categoriaId,
            Integer mes, Integer anio, Pageable paginacion,
            String ipCliente) {

        LocalDateTime[] rango = resolverRangoFechas(mes, anio);

        // Usamos publicarAcceso para consultas de lectura
        publicadorAuditoria.publicarAcceso(
                usuarioId,
                "LISTAR_HISTORIAL",
                "El usuario consultó su historial de transacciones.",
                ipCliente
        );
        return transaccionRepository.buscarConFiltros(
                usuarioId, tipo, categoriaId,
                rango[0], rango[1], paginacion
        ).map(TransaccionDTO::desde);
    }

    @Transactional(readOnly = true)
    public ResumenFinancieroDTO obtenerResumen(UUID usuarioId, Integer mes, Integer anio, String ipCliente) {
        LocalDateTime[] rango = resolverRangoFechas(mes, anio);
        LocalDateTime desde = rango[0];
        LocalDateTime hasta = rango[1];

        BigDecimal totalIngresos = transaccionRepository.sumarIngresosPorPeriodo(usuarioId, desde, hasta);
        BigDecimal totalGastos = transaccionRepository.sumarGastosPorPeriodo(usuarioId, desde, hasta);
        long cantidadIngresos = transaccionRepository.contarPorTipoYPeriodo(usuarioId, TipoMovimiento.INGRESO, desde, hasta);
        long cantidadGastos = transaccionRepository.contarPorTipoYPeriodo(usuarioId, TipoMovimiento.GASTO, desde, hasta);
        
        publicadorAuditoria.publicarAcceso(
                usuarioId,
                "OBTENER_RESUMEN",
                "Se generó el resumen financiero del periodo solicitado.",
                ipCliente
        );
        return ResumenFinancieroDTO.calcular(desde, hasta, totalIngresos, totalGastos, cantidadIngresos, cantidadGastos);
    }

    @Transactional(readOnly = true)
    public TransaccionDTO obtenerPorId(UUID id) {
        return transaccionRepository.findById(id)
                .map(TransaccionDTO::desde)
                .orElseThrow(() -> new IllegalArgumentException("Transacción no encontrada con ID: " + id));
    }

    // ── Privados ─────────────────────────────────────────────────────────────
    private Transaccion construirEntidad(TransaccionRequestDTO request) {
        Categoria categoria = categoriaRepository.findById(request.categoriaId())
                .orElseThrow(() -> new IllegalArgumentException(
                "Categoría no encontrada con ID: " + request.categoriaId()));

        if (categoria.getTipo() != request.tipo()) {
            throw new IllegalArgumentException(String.format(
                    "La categoría '%s' es de tipo %s, pero la transacción es %s.",
                    categoria.getNombre(), categoria.getTipo(), request.tipo()));
        }

        return Transaccion.builder()
                .usuarioId(request.usuarioId())
                .nombreCliente(request.nombreCliente())
                .monto(request.monto())
                .tipo(request.tipo())
                .categoria(categoria)
                .fechaTransaccion(request.fechaTransaccion() != null
                        ? request.fechaTransaccion() : LocalDateTime.now())
                .metodoPago(request.metodoPago())
                .etiquetas(request.etiquetas())
                .notas(request.notas())
                .build();
    }

    private LocalDateTime[] resolverRangoFechas(Integer mes, Integer anio) {
        if (mes == null && anio == null) {
            return new LocalDateTime[]{null, null};
        }

        int anioResuelto = (anio != null) ? anio : LocalDateTime.now().getYear();
        int mesResuelto = (mes != null) ? mes : LocalDateTime.now().getMonthValue();
        YearMonth periodo = YearMonth.of(anioResuelto, mesResuelto);

        return new LocalDateTime[]{
            periodo.atDay(1).atStartOfDay(),
            periodo.atEndOfMonth().atTime(23, 59, 59)
        };
    }
}
