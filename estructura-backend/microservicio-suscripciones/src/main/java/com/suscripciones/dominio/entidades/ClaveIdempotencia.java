package com.suscripciones.dominio.entidades;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Entidad para registrar las claves de idempotencia procesadas.
 * Protege contra ejecuciones duplicadas de pagos en la plataforma.
 */
@Entity
@Table(name = "claves_idempotencia")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClaveIdempotencia {

    @Id
    @Column(length = 255, nullable = false)
    private String clave;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;
}
