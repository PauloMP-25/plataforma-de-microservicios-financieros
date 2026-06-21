package com.libreria.comun.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventoTransaccionRegistradaDTO implements Serializable {
    private String transaccionId;
    private String usuarioId;
    private BigDecimal monto;
    private String tipo; // "INGRESO" o "GASTO"
    private String fechaTransaccion;
}
