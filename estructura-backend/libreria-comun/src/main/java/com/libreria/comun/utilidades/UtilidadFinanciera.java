package com.libreria.comun.utilidades;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Utilidad transversal para el formateo consistente de datos financieros y
 * temporales.
 * 
 * @author Paulo Moron
 */
public final class UtilidadFinanciera {

    @SuppressWarnings("deprecation")
    private static final DateTimeFormatter FORMATO_ES = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss",
            new Locale("es", "PE"));

    private UtilidadFinanciera() {
    }

    /**
     * Formatea una fecha al estándar de lectura en español para la plataforma.
     * 
     * @param fecha LocalDateTime a formatear.
     * @return String formateado (ej: 08/05/2026 14:30:00).
     */
    public static String formatearFecha(LocalDateTime fecha) {
        return (fecha == null) ? "" : fecha.format(FORMATO_ES);
    }

    /**
     * Asegura que un monto financiero tenga exactamente 2 decimales con redondeo
     * hacia arriba.
     * Evita errores de precisión en cálculos de saldos.
     * 
     * @param monto El valor numérico a normalizar.
     * @return BigDecimal con escala 2.
     */
    public static BigDecimal normalizarMonto(BigDecimal monto) {
        if (monto == null)
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        return monto.setScale(2, RoundingMode.HALF_UP);
    }
}
