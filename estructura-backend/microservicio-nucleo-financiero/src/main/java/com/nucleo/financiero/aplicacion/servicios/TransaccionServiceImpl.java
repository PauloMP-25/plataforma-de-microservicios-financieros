package com.nucleo.financiero.aplicacion.servicios;

import com.nucleo.financiero.aplicacion.dtos.solicitudes.SolicitudTransaccion;
import com.nucleo.financiero.aplicacion.dtos.respuestas.ResumenFinancieroDTO;
import com.nucleo.financiero.aplicacion.dtos.respuestas.RespuestaTransaccion;
import com.nucleo.financiero.aplicacion.puertos.ITransaccionService;
import com.nucleo.financiero.aplicacion.mappers.TransaccionMapper;
import com.nucleo.financiero.dominio.entidades.Categoria;
import com.nucleo.financiero.dominio.entidades.Categoria.TipoMovimiento;
import com.nucleo.financiero.dominio.entidades.Transaccion;
import com.nucleo.financiero.dominio.repositorios.CategoriaRepository;
import com.nucleo.financiero.dominio.repositorios.TransaccionRepository;
import com.nucleo.financiero.infraestructura.mensajeria.PublicadorAuditoria;
import com.nucleo.financiero.infraestructura.mensajeria.PublicadorFinanciero;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import com.libreria.comun.excepciones.ExcepcionRecursoNoEncontrado;
import java.util.List;
import java.util.UUID;

/**
 * Implementación de {@link ITransaccionService} para la gestión de movimientos financieros.
 * <p>
 * Aplica lógica de negocio para la validación, persistencia y auditoría de transacciones,
 * integrando repositorios de dominio y publicadores de eventos.
 * </p>
 *
 * @version 1.3.0
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class TransaccionServiceImpl implements ITransaccionService {

    private final TransaccionRepository transaccionRepository;
    private final CategoriaRepository categoriaRepository;
    private final PublicadorAuditoria publicadorAuditoria;
    private final PublicadorFinanciero publicadorFinanciero;
    private final TransaccionMapper transaccionMapper;

    @Override
    @Transactional
    public RespuestaTransaccion registrar(SolicitudTransaccion request, String ipCliente) {
        if (request == null) {
            throw new IllegalArgumentException("La solicitud de transacción no puede ser nula.");
        }
        Transaccion guardada = transaccionRepository.save(construirEntidad(request));
        log.info("Transacción registrada: {} — {} {} ({})",
                guardada.getId(), guardada.getTipo(), guardada.getMonto(), guardada.getNombreCliente());

        publicadorAuditoria.publicarRegistro(
                guardada.getUsuarioId(),
                guardada.getId(),
                guardada.getMonto().toString(),
                ipCliente
        );
        publicadorFinanciero.publicarTransaccionRegistrada(
                guardada.getId(),
                guardada.getUsuarioId(),
                guardada.getMonto(),
                guardada.getTipo().name(),
                guardada.getFechaTransaccion().toString()
        );
        return transaccionMapper.aDto(guardada);
    }

    @Override
    @Transactional
    public List<RespuestaTransaccion> registrarLote(List<SolicitudTransaccion> solicitudes, String ipCliente) {
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
                .toList();
        List<Transaccion> guardadas = transaccionRepository.saveAll(entidades);
        log.info("Lote completado: {} transacciones guardadas", guardadas.size());
        
        publicadorAuditoria.publicarAcceso(
                guardadas.get(0).getUsuarioId(),
                "REGISTRO_LOTE_TRANSACCIONES",
                "Se registraron " + guardadas.size() + " transacciones exitosamente.",
                ipCliente
        );
        for (Transaccion t : guardadas) {
            publicadorFinanciero.publicarTransaccionRegistrada(
                    t.getId(),
                    t.getUsuarioId(),
                    t.getMonto(),
                    t.getTipo().name(),
                    t.getFechaTransaccion().toString()
            );
        }
        return guardadas.stream().map(transaccionMapper::aDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RespuestaTransaccion> listarHistorial(
            UUID usuarioId, TipoMovimiento tipo, UUID categoriaId,
            LocalDateTime desde, LocalDateTime hasta, Pageable paginacion,
            String ipCliente) {

        if (usuarioId == null) {
            throw new IllegalArgumentException("El ID de usuario no puede ser nulo.");
        }
        if (paginacion == null) {
            throw new IllegalArgumentException("La información de paginación no puede ser nula.");
        }

        if (desde == null) {
            desde = LocalDateTime.now().minusDays(30);
        }
        if (hasta == null) {
            hasta = LocalDateTime.now();
        }

        publicadorAuditoria.publicarAcceso(usuarioId, "CONSULTA_HISTORIAL",
                "Rango: " + desde + " a " + hasta, ipCliente);

        org.springframework.data.jpa.domain.Specification<Transaccion> specs = org.springframework.data.jpa.domain.Specification
                .where(com.nucleo.financiero.dominio.especificaciones.TransaccionSpecs.porUsuario(usuarioId))
                .and(com.nucleo.financiero.dominio.especificaciones.TransaccionSpecs.porTipo(tipo))
                .and(com.nucleo.financiero.dominio.especificaciones.TransaccionSpecs.porCategoria(categoriaId))
                .and(com.nucleo.financiero.dominio.especificaciones.TransaccionSpecs.entreFechas(desde, hasta));

        return transaccionRepository.findAll(specs, paginacion).map(transaccionMapper::aDto);
    }

    @Override
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

    @Override
    @Transactional(readOnly = true)
    public RespuestaTransaccion obtenerPorId(UUID id) {
        if (id == null) {
            throw new IllegalArgumentException("El ID de transacción no puede ser nulo.");
        }
        return transaccionRepository.findById(id)
                .map(transaccionMapper::aDto)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("Transaccion", id));
    }

    /**
     * Construye una entidad de dominio {@link Transaccion} a partir de una solicitud DTO.
     * Realiza validaciones de integridad entre categoría y tipo de movimiento.
     * 
     * @param request Datos de la solicitud.
     * @return Entidad de dominio construida.
     * @throws NoSuchElementException Si la categoría no existe.
     * @throws IllegalStateException Si hay inconsistencia entre categoría y tipo.
     */
    private Transaccion construirEntidad(SolicitudTransaccion request) {
        Categoria categoria = obtenerCategoriaValidada(request.categoriaId());
        validarConsistenciaTransaccion(request, categoria);
        
        return Transaccion.builder()
                .usuarioId(request.usuarioId())
                .nombreCliente(request.nombreCliente())
                .monto(request.monto())
                .tipo(request.tipo())
                .categoria(categoria)
                .fechaTransaccion(request.fechaTransaccion() != null ? request.fechaTransaccion() : LocalDateTime.now())
                .metodoPago(request.metodoPago())
                .etiquetas(request.etiquetas())
                .descripcion(request.descripcion())
                .build();
    }

    /**
     * Obtiene y valida la categoría asociada a la transacción.
     */
    private Categoria obtenerCategoriaValidada(UUID categoriaId) {
        if (categoriaId == null) {
            throw new IllegalArgumentException("El ID de la categoría es nulo en la petición.");
        }
        return categoriaRepository.findById(categoriaId)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("Categoria", categoriaId));
    }

    /**
     * Valida la consistencia de tipos entre la transacción y la categoría seleccionada.
     */
    private void validarConsistenciaTransaccion(SolicitudTransaccion request, Categoria categoria) {
        if (request.tipo() == null) {
            throw new IllegalArgumentException("El tipo de movimiento es obligatorio.");
        }
        if (categoria.getTipo() != request.tipo()) {
            throw new IllegalStateException(String.format(
                    "Inconsistencia: La categoría es de tipo %s pero la transacción es %s.",
                    categoria.getTipo(), request.tipo()));
        }
    }

    /**
     * Resuelve el rango de fechas para un mes y año específicos.
     * 
     * @param mes Mes (1-12).
     * @param anio Año (ej: 2026).
     * @return Array con fecha inicio [0] y fecha fin [1].
     */
    private LocalDateTime[] resolverRangoFechas(Integer mes, Integer anio) {
        int anioResuelto = (anio != null) ? anio : LocalDateTime.now().getYear();
        int mesResuelto = (mes != null) ? mes : LocalDateTime.now().getMonthValue();
        YearMonth periodo = YearMonth.of(anioResuelto, mesResuelto);

        return new LocalDateTime[]{
            periodo.atDay(1).atStartOfDay(),
            LocalDateTime.now()
        };
    }
}
