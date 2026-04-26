package com.comun.aplicacion.dtos;

import lombok.*;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RespuestaError {
    private LocalDateTime timestamp;
    private int status;
    private String error;
    private String message;
    private String path;
}
