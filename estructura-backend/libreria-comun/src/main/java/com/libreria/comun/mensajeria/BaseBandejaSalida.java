package com.libreria.comun.mensajeria;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Superclase mapeada JPA estándar para el patrón Outbox.
 * Define la estructura común para la persistencia confiable de mensajes en todo el ecosistema LUKA.
 */
@MappedSuperclass
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public abstract class BaseBandejaSalida {

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
