package com.suscripciones.dominio.entidades;

import com.libreria.comun.autoconfiguracion.Auditable;
import com.libreria.comun.autoconfiguracion.AuditoriaListener;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidad que registra el histórico de pagos confirmados para las suscripciones.
 */
@Entity
@Table(name = "historial_pagos_suscripcion", indexes = {
    @Index(name = "idx_historial_suscripcion", columnList = "suscripcion_id"),
    @Index(name = "idx_historial_transaccion", columnList = "transaccion_id")
})
@Auditable
@EntityListeners(AuditoriaListener.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HistorialPagoSuscripcion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "suscripcion_id", nullable = false)
    private Suscripcion suscripcion;

    @Column(name = "transaccion_id")
    private UUID transaccionId; // Enlace a la transacción registrada en el núcleo financiero (se poblará de forma asíncrona por el Outbox)

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal monto;

    @Column(name = "fecha_pago", nullable = false)
    private java.time.LocalDate fechaPago;

    @Column(nullable = false, length = 20)
    private String estado; // e.g. EXITOSO, FALLIDO, DEVOLUCION

    @Column(name = "fecha_creacion", updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;
}
