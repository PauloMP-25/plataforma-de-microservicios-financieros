package com.libreria.comun.utilidades;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Utilidad para la gestión y extracción de direcciones IP en entornos distribuidos.
 * <p>
 * En una arquitectura de microservicios, la IP real suele venir oculta tras el 
 * API Gateway en la cabecera 'X-Forwarded-For'. Esta clase asegura que se 
 * recupere la IP del cliente original y no la del proxy.
 * </p>
 * 
 * @author Paulo Moron
 */
public final class UtilidadIp {

    private UtilidadIp() {}

    /**
     * Extrae la dirección IP real del cliente desde la petición HTTP.
     * 
     * @param request La petición servlet entrante.
     * @return String con la IP (IPv4 o IPv6).
     */
    public static String obtenerIpReal(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        } else {
            // Si hay una cadena de IPs, la primera es la del cliente original
            ip = ip.split(",")[0].trim();
        }
        return "0:0:0:0:0:0:0:1".equals(ip) ? "127.0.0.1" : ip;
    }
}
