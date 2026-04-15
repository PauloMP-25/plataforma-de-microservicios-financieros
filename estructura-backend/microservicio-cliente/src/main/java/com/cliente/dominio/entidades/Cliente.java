package com.cliente.dominio.entidades;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidad principal de perfil de cliente.
 * usuarioId referencia al usuario del microservicio IAM.
 */
@Entity
@Table(
    name = "clientes",
    indexes = {
        @Index(name = "idx_cliente_usuario_id", columnList = "usuario_id"),
        @Index(name = "idx_cliente_dni",        columnList = "dni")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    /** Referencia al usuario del microservicio IAM */
    @Column(name = "usuario_id", nullable = false, unique = true)
    private UUID usuarioId;

    @Column(length = 100)
    private String nombres;

    @Column(length = 100)
    private String apellidos;

    @Column(unique = true, length = 8)
    private String dni;

    @Column(name = "foto_perfil_url", length = 500)
    private String fotoPerfilUrl;

    @Column(columnDefinition = "TEXT")
    private String biografia;

    @Column(name = "numero_celular", length = 15)
    private String numeroCelular;

    @Column(length = 255)
    private String direccion;

    @Column(length = 100)
    private String ciudad;

    @Column(length = 100)
    private String ocupacion;

    @Column(length = 20)
    private String genero;

    @Column(name = "perfil_completo", nullable = false)
    @Builder.Default
    private Boolean perfilCompleto = false;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion", nullable = false)
    private LocalDateTime fechaActualizacion;

    @PrePersist
    protected void alCrear() {
        fechaCreacion     = LocalDateTime.now();
        fechaActualizacion = LocalDateTime.now();
    }

    @PreUpdate
    protected void alActualizar() {
        fechaActualizacion = LocalDateTime.now();
    }

    /** Evalúa si el perfil tiene los campos mínimos completados. */
    public boolean evaluarPerfilCompleto() {
        return nombres      != null && !nombres.isBlank()
            && apellidos    != null && !apellidos.isBlank()
            && dni          != null && !dni.isBlank()
            && numeroCelular != null && !numeroCelular.isBlank()
            && ciudad       != null && !ciudad.isBlank();
    }
}
