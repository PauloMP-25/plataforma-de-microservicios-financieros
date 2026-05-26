package com.nucleo.financiero.dominio.entidades;

import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

/**
 * Entidad de dominio que representa una categoría de gasto o ingreso.
 * <p>
 * Permite organizar los movimientos financieros del usuario (ej: Alimentación,
 * Sueldo, Entretenimiento) para facilitar el análisis por parte de la IA.
 * </p>
 * 
 * @author Luka-Dev-Backend
 * @version 1.1.0
 */
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

    /**
     * Define la naturaleza financiera de la categoría.
     */
    public enum TipoMovimiento {
        /** Entradas de dinero al patrimonio del usuario */
        INGRESO,
        /** Salidas o flujos negativos de dinero */
        GASTO
    }
}