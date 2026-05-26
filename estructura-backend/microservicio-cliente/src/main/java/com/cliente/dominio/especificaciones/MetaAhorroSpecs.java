package com.cliente.dominio.especificaciones;

import com.cliente.dominio.entidades.MetaAhorro;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

/**
 * Motor de especificaciones para consultas dinámicas y escalables sobre Metas de Ahorro.
 * Permite combinar criterios de búsqueda sin ensuciar los repositorios con múltiples métodos.
 */
public class MetaAhorroSpecs {

    public static Specification<MetaAhorro> perteneceAUsuario(UUID usuarioId) {
        return (root, query, cb) -> cb.equal(root.get("usuarioId"), usuarioId);
    }

    public static Specification<MetaAhorro> estaCompletada(boolean completada) {
        return (root, query, cb) -> cb.equal(root.get("completada"), completada);
    }

    public static Specification<MetaAhorro> venceAntesDe(java.time.LocalDate fecha) {
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("fechaLimite"), fecha);
    }

    public static Specification<MetaAhorro> tieneProgresoBajo(double porcentaje) {
        return (root, query, cb) -> {
            // montoActual / montoObjetivo < porcentaje
            return cb.lessThan(
                cb.quot(root.<Number>get("montoActual"), root.<Number>get("montoObjetivo")).as(Double.class), 
                porcentaje
            );
        };
    }
}
