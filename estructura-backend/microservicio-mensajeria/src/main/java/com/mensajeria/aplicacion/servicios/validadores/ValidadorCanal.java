package com.mensajeria.aplicacion.servicios.validadores;

import com.mensajeria.aplicacion.dtos.solicitudes.SolicitudGenerarCodigo;
import com.mensajeria.aplicacion.excepciones.CodigoInvalidoException;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(1)
public class ValidadorCanal implements ValidadorOtp {

    @Override
    public void validar(SolicitudGenerarCodigo solicitud) throws RuntimeException {
        if (solicitud.tipo() == com.libreria.comun.enums.TipoVerificacion.EMAIL) {
            if (solicitud.email() == null || solicitud.email().isBlank()) {
                throw new CodigoInvalidoException("el canal EMAIL requiere un email válido");
            }
        } else {
            if (solicitud.telefono() == null || solicitud.telefono().isBlank()) {
                throw new CodigoInvalidoException("el canal SMS/WHATSAPP requiere un teléfono en formato E.164");
            }
        }
    }
}
