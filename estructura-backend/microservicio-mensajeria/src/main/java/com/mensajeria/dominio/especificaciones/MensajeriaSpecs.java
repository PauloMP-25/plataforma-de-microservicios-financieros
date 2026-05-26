package com.mensajeria.dominio.especificaciones;

import com.mensajeria.dominio.entidades.CodigoVerificacion;
import com.libreria.comun.enums.PropositoCodigo;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Especificaciones dinámicas para la búsqueda de códigos de verificación.
 * Implementa el Specification Pattern para auditoría y gestión de OTPs.
 * 
 * @author Paulo Moron
 * @version 1.0.0
 */
public class MensajeriaSpecs {

    /**
     * Filtra por el ID del usuario.
     */
    public static Specification<CodigoVerificacion> porUsuario(UUID usuarioId) {
        return (root, query, cb) -> 
            usuarioId == null ? null : cb.equal(root.get("usuarioId"), usuarioId);
    }

    /**
     * Filtra por el propósito del código (ACTIVACION, RESET, etc).
     */
    public static Specification<CodigoVerificacion> porProposito(PropositoCodigo proposito) {
        return (root, query, cb) -> 
            proposito == null ? null : cb.equal(root.get("proposito"), proposito);
    }

    /**
     * Filtra códigos que ya han sido usados o están pendientes.
     */
    public static Specification<CodigoVerificacion> estaUsado(Boolean usado) {
        return (root, query, cb) -> 
            usado == null ? null : cb.equal(root.get("usado"), usado);
    }

    /**
     * Filtra códigos creados en un rango de fechas.
     */
    public static Specification<CodigoVerificacion> creadoEntre(LocalDateTime inicio, LocalDateTime fin) {
        return (root, query, cb) -> {
            if (inicio == null && fin == null) return null;
            if (inicio != null && fin != null) return cb.between(root.get("fechaCreacion"), inicio, fin);
            if (inicio != null) return cb.greaterThanOrEqualTo(root.get("fechaCreacion"), inicio);
            return cb.lessThanOrEqualTo(root.get("fechaCreacion"), fin);
        };
    }
}
