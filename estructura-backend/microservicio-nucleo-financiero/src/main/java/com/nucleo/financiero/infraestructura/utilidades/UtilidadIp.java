package com.nucleo.financiero.infraestructura.utilidades;

import jakarta.servlet.http.HttpServletRequest;

public class UtilidadIp {

    public static String obtenerIpRemota(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        } else {
            // En caso de múltiples proxies, tomamos la primera IP
            ip = ip.split(",")[0];
        }
        return ip;
    }
}
