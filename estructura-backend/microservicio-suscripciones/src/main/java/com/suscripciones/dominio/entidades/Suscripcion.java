package com.suscripciones.dominio.entidades;

import com.libreria.comun.autoconfiguracion.Auditable;
import com.libreria.comun.autoconfiguracion.AuditoriaListener;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidad principal que representa una suscripción activa o histórica de un usuario en LUKA APP.
 */
@Entity
@Table(name = "suscripciones", indexes = {
    @Index(name = "idx_suscripcion_usuario", columnList = "usuario_id"),
    @Index(name = "idx_suscripcion_estado", columnList = "estado")
})
@SQLDelete(sql = "UPDATE suscripciones SET eliminado = true WHERE id = ?")
@SQLRestriction("eliminado = false")
@Auditable
@EntityListeners(AuditoriaListener.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Suscripcion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "usuario_id", nullable = false)
    private UUID usuarioId;

    @Column(nullable = false, length = 100)
    private String nombre; // e.g. Netflix, Spotify, Luka Premium

    @Column(name = "categoria_id")
    private UUID categoriaId;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal monto;

    @Column(nullable = false, length = 20)
    private String estado; // e.g. ACTIVA, VENCIDA, CANCELADA

    @Column(name = "metodo_pago", nullable = false, length = 50)
    private String metodoPago; // e.g. STRIPE, MANUAL

    @Column(name = "fecha_inicio", nullable = false)
    private java.time.LocalDate fechaInicio;

    @Column(name = "fecha_vencimiento", nullable = false)
    private java.time.LocalDate fechaVencimiento;

    @Column(name = "fecha_ultimo_pago")
    private java.time.LocalDate fechaUltimoPago;

    @Column(name = "tipo_estrategia", nullable = false, length = 30)
    private String tipoEstrategia; // e.g. CALENDARIO, DIAS_HABILES

    @Builder.Default
    @Column(nullable = false)
    private boolean eliminado = false;

    @Column(name = "fecha_creacion", updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;
}
