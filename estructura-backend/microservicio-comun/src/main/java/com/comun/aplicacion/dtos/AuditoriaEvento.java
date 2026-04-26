package com.comun.aplicacion.dtos;

import lombok.*;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AuditoriaEvento implements Serializable {
    private String usuarioID;
    private String accion;      // Ejemplo: "LOGIN_FALLIDO", "REGISTRO_USUARIO"
    private String modulo;      // Ejemplo: "MICRO-USUARIOS"
    private String detalles;
    private String ip;
    private LocalDateTime timestamp;
}
