package com.cliente.dominio.entidades;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidad que almacena los datos de identificación personal del usuario.
 * Relación 1:1 con el usuarioId del microservicio IAM.
 */
@Entity
@Table(
    name = "datos_personales",
    indexes = {
        @Index(name = "idx_datos_personales_usuario_id", columnList = "usuario_id"),
        @Index(name = "idx_datos_personales_dni",        columnList = "dni")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DatosPersonales {

    @Id
    @GeneratedValue
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;
    

    /** Referencia al usuario del microservicio IAM — único y obligatorio */
    @Column(name = "usuario_id", nullable = false, unique = true)
    private UUID usuarioId;

    @Column(unique = true, length = 8)
    private String dni;

    @Column(length = 100)
    private String nombres;

    @Column(length = 100)
    private String apellidos;

    /**
     * Valores esperados: MASCULINO, FEMENINO, OTRO, PREFIERO_NO_DECIR
     */
    @Column(length = 20)
    private String genero;

    @Column
    private Integer edad;

    @Column(name = "telefono", length = 15)
    private String telefono;

    @Column(name = "foto_perfil_url", length = 500)
    private String fotoPerfilUrl;

    @Column(length = 100)
    private String pais;

    @Column(length = 100)
    private String ciudad;

    @Column(name = "datos_completos", nullable = false)
    @Builder.Default
    private Boolean datosCompletos = false;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion", nullable = false)
    private LocalDateTime fechaActualizacion;

    @PrePersist
    protected void alCrear() {
        fechaCreacion      = LocalDateTime.now();
        fechaActualizacion = LocalDateTime.now();
    }

    @PreUpdate
    protected void alActualizar() {
        fechaActualizacion = LocalDateTime.now();
    }

    /** Verifica que los campos mínimos obligatorios estén presentes.
     * @return  */
    public boolean evaluarDatosCompletos() {
        return nombres   != null && !nombres.isBlank()
            && apellidos != null && !apellidos.isBlank()
            && dni       != null && !dni.isBlank()
            && telefono  != null && !telefono.isBlank()
            && ciudad    != null && !ciudad.isBlank();
    }

    /** Nombre completo para uso en auditoría y logs.
     * @return  */
    public String obtenerNombreCompleto() {
        String n = nombres   != null ? nombres.trim()   : "";
        String a = apellidos != null ? apellidos.trim() : "";
        return (n + " " + a).trim();
    }
}
