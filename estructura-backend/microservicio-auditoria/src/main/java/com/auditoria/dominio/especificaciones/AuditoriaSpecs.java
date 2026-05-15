package com.auditoria.dominio.especificaciones;

import com.auditoria.dominio.entidades.AuditoriaTransaccional;
import com.auditoria.dominio.entidades.RegistroAuditoria;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;

/**
 * Especificaciones dinámicas para el filtrado de registros de auditoría.
 * Implementa el Specification Pattern para búsquedas avanzadas y modulares.
 * 
 * @author Paulo Moron
 * @version 1.0.0
 */
public class AuditoriaSpecs {

    // --- Especificaciones para RegistroAuditoria ---

    /**
     * Filtra por el módulo que generó el evento (case-insensitive).
     */
    public static Specification<RegistroAuditoria> registroPorModulo(String modulo) {
        return (root, query, cb) -> 
            (modulo == null || modulo.isEmpty()) ? null : 
            cb.equal(cb.lower(root.get("modulo")), modulo.toLowerCase());
    }

    /**
     * Filtra por nivel de severidad o tipo de evento.
     */
    public static Specification<RegistroAuditoria> registroPorTipo(String tipo) {
        return (root, query, cb) -> 
            (tipo == null || tipo.isEmpty()) ? null : cb.equal(root.get("tipoEvento"), tipo);
    }

    // --- Especificaciones para AuditoriaTransaccional ---

    /**
     * Filtra por el microservicio de origen de la transacción.
     */
    public static Specification<AuditoriaTransaccional> transaccionPorServicio(String servicio) {
        return (root, query, cb) -> 
            (servicio == null || servicio.isEmpty()) ? null : cb.equal(root.get("servicioOrigen"), servicio);
    }

    /**
     * Filtra por un rango de fechas.
     */
    public static Specification<AuditoriaTransaccional> transaccionEntreFechas(LocalDateTime desde, LocalDateTime hasta) {
        return (root, query, cb) -> {
            if (desde == null && hasta == null) return null;
            if (desde != null && hasta != null) return cb.between(root.get("fecha"), desde, hasta);
            if (desde != null) return cb.greaterThanOrEqualTo(root.get("fecha"), desde);
            return cb.lessThanOrEqualTo(root.get("fecha"), hasta);
        };
    }

    /**
     * Filtra por el tipo de operación (CREATE, UPDATE, DELETE).
     */
    public static Specification<AuditoriaTransaccional> transaccionPorOperacion(String operacion) {
        return (root, query, cb) -> 
            (operacion == null || operacion.isEmpty()) ? null : cb.equal(root.get("operacion"), operacion);
    }
}
