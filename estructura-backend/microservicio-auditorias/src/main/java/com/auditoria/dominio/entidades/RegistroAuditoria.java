package com.auditoria.dominio.entidades;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
    name = "registros_auditoria",
    indexes = {
        @Index(name = "idx_auditoria_modulo",    columnList = "modulo"),
        @Index(name = "idx_auditoria_fecha",     columnList = "fecha_hora"),
        @Index(name = "idx_auditoria_usuario",   columnList = "nombre_usuario")
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegistroAuditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime fechaHora;

    @Column(name = "nombre_usuario", nullable = false, length = 150)
    private String nombreUsuario;

    @Column(name = "accion", nullable = false, length = 100)
    private String accion;

    @Column(name = "modulo", nullable = false, length = 100)
    private String modulo;

    @Column(name = "ip_origen", length = 45)
    private String ipOrigen;

    @Column(name = "detalles", columnDefinition = "TEXT")
    private String detalles;

    @PrePersist
    protected void alCrear() {
        if (this.fechaHora == null) {
            this.fechaHora = LocalDateTime.now();
        }
    }
}
