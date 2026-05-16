package com.pagos.dominio.especificaciones;

import com.pagos.dominio.entidades.Boleta;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;

/**
 * Especificaciones dinámicas para la búsqueda y filtrado de Boletas.
 */
public class BoletaSpecs {

    /**
     * Filtra boletas por el email del receptor.
     */
    public static Specification<Boleta> porEmailReceptor(String email) {
        return (root, query, cb) -> 
            (email == null || email.isEmpty()) ? null : cb.equal(root.get("emailReceptor"), email);
    }

    /**
     * Filtra boletas por su código único.
     */
    public static Specification<Boleta> porCodigoBoleta(String codigo) {
        return (root, query, cb) -> 
            (codigo == null || codigo.isEmpty()) ? null : cb.equal(root.get("codigoBoleta"), codigo);
    }

    /**
     * Filtra boletas emitidas en un rango de fechas.
     */
    public static Specification<Boleta> emitidasEntre(LocalDateTime inicio, LocalDateTime fin) {
        return (root, query, cb) -> {
            if (inicio == null && fin == null) return null;
            if (inicio != null && fin != null) return cb.between(root.get("fechaEmision"), inicio, fin);
            if (inicio != null) return cb.greaterThanOrEqualTo(root.get("fechaEmision"), inicio);
            return cb.lessThanOrEqualTo(root.get("fechaEmision"), fin);
        };
    }
}
