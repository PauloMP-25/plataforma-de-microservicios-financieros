package com.pagos.dominio.entidades;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidad para el patrón Outbox.
 * Almacena eventos que deben ser publicados de forma fiable.
 */
@Entity
@Table(name = "bandeja_salida")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BandejaSalida {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tipo_evento", nullable = false)
    private String tipoEvento;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String payload;

    @Column(nullable = false)
    private boolean procesado;

    @Column(nullable = false)
    private int intentos;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_proceso")
    private LocalDateTime fechaProceso;

    @PrePersist
    protected void onCreate() {
        fechaCreacion = LocalDateTime.now();
        procesado = false;
        intentos = 0;
    }
}
