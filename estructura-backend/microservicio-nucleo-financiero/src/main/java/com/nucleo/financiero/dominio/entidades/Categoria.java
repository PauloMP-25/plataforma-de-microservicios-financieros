package com.nucleo.financiero.dominio.entidades;

import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Entity
@Table(
    name = "categorias",
    indexes = {
        @Index(name = "idx_categoria_tipo", columnList = "tipo"),
        @Index(name = "idx_categoria_nombre", columnList = "nombre")
    }
)
@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Categoria {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(nullable = false, unique = true, length = 100)
    private String nombre;

    @Column(length = 255)
    private String descripcion;

    @Column(length = 50)
    private String icono;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TipoMovimiento tipo;

    public enum TipoMovimiento {
        INGRESO,
        GASTO
    }
}
