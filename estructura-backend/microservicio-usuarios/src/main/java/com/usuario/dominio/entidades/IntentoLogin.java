package com.usuario.dominio.entidades;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
/**
 * Registra los intentos de login fallidos por IP.
 * Persiste en BD para sobrevivir reinicios del servicio.
 * Un HashMap concurrente en memoria actúa como caché de primer nivel.
 * @author Paulo
 */
@Entity
@Table(name = "intentos_login")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IntentoLogin {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "direccion_ip", nullable = false, unique = true, length = 50)
    private String direccionIp;

    @Column(nullable = false)
    @Builder.Default
    private int intentos = 0;

    @Column(name = "ultima_modificacion", nullable = false)
    private LocalDateTime ultimaModificacion;
    
    @Column(name = "bloqueado", nullable = false)
    @Builder.Default
    private boolean bloqueado = false;

    @Column(name = "bloqueado_hasta")
    private LocalDateTime bloqueadoHasta;

    // -------------------------------------------------------------------------
    // Métodos de dominio
    // -------------------------------------------------------------------------

    /**
     * Incrementa el contador de intentos y actualiza la marca de tiempo.
     */
    public void incrementarIntentos() {
        this.intentos++;
        this.ultimaModificacion = LocalDateTime.now();
    }
    
    /**
     * Bloquea la IP por las horas indicadas.
     *
     * @param horas duración del bloqueo en horas
     */
    public void bloquear(long horas) {
        this.bloqueado = true;
        this.bloqueadoHasta = LocalDateTime.now().plusHours(horas);
        this.ultimaModificacion = LocalDateTime.now();
    }
    
    /**
     * Resetea el estado de la IP (login exitoso o ventana expirada).
     */
    public void reiniciar() {
        this.intentos = 0;
        this.bloqueado = false;
        this.bloqueadoHasta = null;
        this.ultimaModificacion = LocalDateTime.now();
    }

    /**
     * Evalúa si el bloqueo ha expirado.
     */
    public boolean bloqueoExpirado() {
        return bloqueadoHasta != null && LocalDateTime.now().isAfter(bloqueadoHasta);
    }
    
    @PrePersist
    protected void alCrear() {
        if (ultimaModificacion == null) {
            ultimaModificacion = LocalDateTime.now();
        }
    }
}
