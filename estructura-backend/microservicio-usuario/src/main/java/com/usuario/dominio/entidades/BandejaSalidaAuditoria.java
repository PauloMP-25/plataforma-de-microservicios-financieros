package com.usuario.dominio.entidades;

import com.libreria.comun.mensajeria.BaseBandejaSalida;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidad local para el patrón Outbox aplicada a eventos de Auditoría.
 * Hereda todos sus campos, mapeos de columnas JPA y métodos de ciclo de vida 
 * de la clase BaseBandejaSalida centralizada en la librería común.
 */
@Entity
@Table(name = "bandeja_salida_auditoria")
@Getter
@Setter
@NoArgsConstructor
public class BandejaSalidaAuditoria extends BaseBandejaSalida {

    @Builder
    public BandejaSalidaAuditoria(
            UUID id,
            String tipoEvento,
            String payload,
            boolean procesado,
            int intentos,
            LocalDateTime fechaCreacion,
            LocalDateTime fechaProceso) {
        super(id, tipoEvento, payload, procesado, intentos, fechaCreacion, fechaProceso);
    }
}
