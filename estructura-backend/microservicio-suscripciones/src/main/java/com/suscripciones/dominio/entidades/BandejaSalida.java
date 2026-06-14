package com.suscripciones.dominio.entidades;

import com.libreria.comun.mensajeria.BaseBandejaSalida;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidad de la bandeja de salida (Outbox) para guardar de forma transaccional
 * los eventos de suscripciones antes de ser publicados en RabbitMQ.
 */
@Entity
@Table(name = "bandeja_salida")
@NoArgsConstructor
public class BandejaSalida extends BaseBandejaSalida {

    public BandejaSalida(String tipoEvento, String payload) {
        this.setTipoEvento(tipoEvento);
        this.setPayload(payload);
        this.setProcesado(false);
        this.setIntentos(0);
        this.setFechaCreacion(LocalDateTime.now());
    }

    /**
     * Constructor completo para soporte de constructores personalizados.
     */
    public BandejaSalida(UUID id, String tipoEvento, String payload, boolean procesado, int intentos, LocalDateTime fechaCreacion, LocalDateTime fechaProceso) {
        super(id, tipoEvento, payload, procesado, intentos, fechaCreacion, fechaProceso);
    }
}
