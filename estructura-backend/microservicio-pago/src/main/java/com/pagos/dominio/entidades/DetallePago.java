package com.pagos.dominio.entidades;

import com.pagos.aplicacion.enums.PlanSuscripcion;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Representa los detalles específicos de un pago.
 */
@Entity
@Table(name = "detalles_pago", indexes = {
    @Index(name = "idx_detalle_pago_id", columnList = "pago_id")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DetallePago {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pago_id", nullable = false)
    private Pago pago;

    @Enumerated(EnumType.STRING)
    @Column(name = "plan_solicitado", nullable = false, length = 20)
    private PlanSuscripcion planSolicitado;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal monto;

    @Column(nullable = false, length = 3)
    private String moneda;

    @Column(length = 255)
    private String descripcion;

    @Column(precision = 10, scale = 2)
    private BigDecimal descuento;

    @Column(nullable = false)
    private Integer cantidad;
}
