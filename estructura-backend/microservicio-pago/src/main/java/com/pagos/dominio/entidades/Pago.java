package com.pagos.dominio.entidades;

import com.pagos.aplicacion.enums.EstadoPago;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Entidad principal que representa un intento o transacción de pago en LUKA APP.
 * Mapea a la tabla 'pagos' en la base de datos exclusiva del microservicio.
 *
 * @author LUKA APP Team
 */
@Entity
@Table(name = "pagos", indexes = {
    @Index(name = "idx_pago_usuario", columnList = "usuario_id"),
    @Index(name = "idx_pago_estado", columnList = "estado")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Pago {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "usuario_id", nullable = false)
    private UUID usuarioId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoPago estado;

    @Column(name = "stripe_session_id", unique = true, length = 255)
    private String stripeSessionId;

    /** ID único del evento Stripe — garantiza idempotencia en webhooks. */
    @Column(name = "stripe_evento_id", unique = true, length = 255)
    private String stripeEventoId;

    @Column(name = "fecha_inicio_plan")
    private LocalDateTime fechaInicioPlan;

    @Column(name = "fecha_fin_plan")
    private LocalDateTime fechaFinPlan;

    @OneToMany(mappedBy = "pago", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DetallePago> detalles;

    @OneToOne(mappedBy = "pago", cascade = CascadeType.ALL)
    private Boleta boleta;

    @CreationTimestamp
    @Column(name = "fecha_creacion", updatable = false)
    private LocalDateTime fechaCreacion;

    @UpdateTimestamp
    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;
}
