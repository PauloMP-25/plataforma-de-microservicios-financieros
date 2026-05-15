package com.pagos.dominio.entidades;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidad principal que representa un intento o transacción de pago en LUKA APP.
 * Mapea a la tabla 'pagos' en la base de datos exclusiva del microservicio.
 *
 * @author LUKA APP Team
 */
@Entity
@Table(name = "pagos")
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

    @Column(name = "email_usuario")
    private String emailUsuario;

    @Enumerated(EnumType.STRING)
    @Column(name = "plan_solicitado", nullable = false, length = 20)
    private PlanSuscripcion planSolicitado;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal monto;

    @Column(nullable = false, length = 3)
    private String moneda;

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

    @CreationTimestamp
    @Column(name = "fecha_creacion", updatable = false)
    private LocalDateTime fechaCreacion;

    @UpdateTimestamp
    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;
}
