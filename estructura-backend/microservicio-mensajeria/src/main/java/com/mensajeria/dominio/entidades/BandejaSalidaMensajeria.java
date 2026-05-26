package com.mensajeria.dominio.entidades;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidad que implementa el Patrón Outbox para garantizar la entrega de
 * eventos a RabbitMQ sin riesgo de pérdida por caídas del broker.
 */
@Entity
@Table(name = "bandeja_salida_mensajeria")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BandejaSalidaMensajeria {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "tipo_evento", nullable = false)
    private String tipoEvento;

    @Column(columnDefinition = "text", nullable = false)
    private String payload;

    @Builder.Default
    private boolean procesado = false;

    @Builder.Default
    private int intentos = 0;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @PrePersist
    protected void alCrear() {
        if (fechaCreacion == null) {
            fechaCreacion = LocalDateTime.now();
        }
    }
}
