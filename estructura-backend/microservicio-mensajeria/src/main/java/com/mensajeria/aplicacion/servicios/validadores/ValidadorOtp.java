package com.mensajeria.aplicacion.servicios.validadores;

import com.mensajeria.aplicacion.dtos.solicitudes.SolicitudGenerarCodigo;

/**
 * Interfaz para el patrón Chain of Responsibility aplicado a la validación
 * de solicitudes de OTP.
 */
public interface ValidadorOtp {
    void validar(SolicitudGenerarCodigo solicitud) throws RuntimeException;
}
