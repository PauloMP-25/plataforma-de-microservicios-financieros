package com.nucleo.financiero.dominio.entidades;

import com.nucleo.financiero.dominio.entidades.Categoria.TipoMovimiento;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
    name = "transacciones",
    indexes = {
        @Index(name = "idx_transaccion_usuario",   columnList = "usuario_id"),
        @Index(name = "idx_transaccion_categoria", columnList = "categoria_id"),
        @Index(name = "idx_transaccion_fecha",     columnList = "fecha_transaccion"),
        @Index(name = "idx_transaccion_tipo",      columnList = "tipo"),
        @Index(name = "idx_transaccion_cliente",   columnList = "nombre_cliente")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaccion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "usuario_id", nullable = false)
    private UUID usuarioId;

    @Column(name = "nombre_cliente", nullable = false, length = 150)
    private String nombreCliente;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal monto;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TipoMovimiento tipo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoria_id", nullable = false)
    private Categoria categoria;

    @Column(name = "fecha_transaccion", nullable = false)
    private LocalDateTime fechaTransaccion;

    @Enumerated(EnumType.STRING)
    @Column(name = "metodo_pago", nullable = false, length = 20)
    private MetodoPago metodoPago;

    @Column(name = "etiquetas", length = 300)
    private String etiquetas;

    @Column(name = "notas", columnDefinition = "TEXT")
    private String notas;

    @Column(name = "fecha_registro", nullable = false, updatable = false)
    private LocalDateTime fechaRegistro;

    public enum MetodoPago {
        EFECTIVO,
        TARJETA,
        TRANSFERENCIA,
        DIGITAL
    }

    @PrePersist
    protected void alCrear() {
        if (fechaRegistro == null) {
            fechaRegistro = LocalDateTime.now();
        }
        if (fechaTransaccion == null) {
            fechaTransaccion = LocalDateTime.now();
        }
    }
}
