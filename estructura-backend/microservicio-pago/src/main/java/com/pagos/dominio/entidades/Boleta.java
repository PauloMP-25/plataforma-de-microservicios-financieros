package com.pagos.dominio.entidades;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidad para gestionar las boletas de pago que se envían al usuario.
 */
@Entity
@Table(name = "boletas", indexes = {
    @Index(name = "idx_boleta_pago_id", columnList = "pago_id")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Boleta {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pago_id", nullable = false, unique = true)
    private Pago pago;

    @Column(name = "codigo_boleta", nullable = false, unique = true, length = 50)
    private String codigoBoleta;

    @Column(name = "email_receptor", nullable = false)
    private String emailReceptor;

    @Column(name = "monto_total", nullable = false, precision = 10, scale = 2)
    private BigDecimal montoTotal;

    @CreationTimestamp
    @Column(name = "fecha_emision", updatable = false)
    private LocalDateTime fechaEmision;

    @Column(name = "enviada_correo")
    private boolean enviadaCorreo = false;
}
