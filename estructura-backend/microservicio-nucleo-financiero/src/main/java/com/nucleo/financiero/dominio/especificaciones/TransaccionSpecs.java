package com.nucleo.financiero.dominio.especificaciones;

import com.nucleo.financiero.dominio.entidades.Categoria.TipoMovimiento;
import com.nucleo.financiero.dominio.entidades.Transaccion;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Especificaciones dinámicas para filtrar transacciones financieras.
 * Implementa el patrón Specification para desacoplar la lógica de filtrado del repositorio.
 */
public class TransaccionSpecs {

    public static Specification<Transaccion> porUsuario(UUID usuarioId) {
        return (root, query, cb) -> cb.equal(root.get("usuarioId"), usuarioId);
    }

    public static Specification<Transaccion> porTipo(TipoMovimiento tipo) {
        return (root, query, cb) -> tipo == null ? null : cb.equal(root.get("tipo"), tipo);
    }

    public static Specification<Transaccion> porCategoria(UUID categoriaId) {
        return (root, query, cb) -> categoriaId == null ? null : cb.equal(root.get("categoria").get("id"), categoriaId);
    }

    public static Specification<Transaccion> entreFechas(LocalDateTime desde, LocalDateTime hasta) {
        return (root, query, cb) -> {
            if (desde == null && hasta == null) return null;
            if (desde != null && hasta != null) return cb.between(root.get("fechaTransaccion"), desde, hasta);
            if (desde != null) return cb.greaterThanOrEqualTo(root.get("fechaTransaccion"), desde);
            return cb.lessThanOrEqualTo(root.get("fechaTransaccion"), hasta);
        };
    }
}
