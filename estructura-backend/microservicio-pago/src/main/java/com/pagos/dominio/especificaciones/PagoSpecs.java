package com.pagos.dominio.especificaciones;

import com.pagos.aplicacion.enums.EstadoPago;
import com.pagos.dominio.entidades.Pago;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Especificaciones dinámicas para la búsqueda y filtrado de Pagos.
 * Implementa el Specification Pattern para desacoplar la lógica de consulta del repositorio.
 * 
 * @author LUKA APP Team
 * @version 1.0.0
 */
public class PagoSpecs {

    /**
     * Filtra pagos por el ID del usuario.
     */
    public static Specification<Pago> porUsuarioId(UUID usuarioId) {
        return (root, query, cb) -> 
            usuarioId == null ? null : cb.equal(root.get("usuarioId"), usuarioId);
    }

    /**
     * Filtra pagos por su estado actual.
     */
    public static Specification<Pago> conEstado(EstadoPago estado) {
        return (root, query, cb) -> 
            estado == null ? null : cb.equal(root.get("estado"), estado);
    }

    /**
     * Filtra pagos creados en un rango de fechas.
     */
    public static Specification<Pago> creadoEntre(LocalDateTime inicio, LocalDateTime fin) {
        return (root, query, cb) -> {
            if (inicio == null && fin == null) return null;
            if (inicio != null && fin != null) return cb.between(root.get("fechaCreacion"), inicio, fin);
            if (inicio != null) return cb.greaterThanOrEqualTo(root.get("fechaCreacion"), inicio);
            return cb.lessThanOrEqualTo(root.get("fechaCreacion"), fin);
        };
    }

    /**
     * Filtra por el ID de sesión de Stripe.
     */
    public static Specification<Pago> porStripeSessionId(String sessionId) {
        return (root, query, cb) -> 
            (sessionId == null || sessionId.isEmpty()) ? null : cb.equal(root.get("stripeSessionId"), sessionId);
    }
}
