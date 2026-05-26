package com.mensajeria.aplicacion.servicios.validadores;

import com.libreria.comun.enums.TipoVerificacion;
import com.mensajeria.aplicacion.dtos.solicitudes.SolicitudGenerarCodigo;
import com.mensajeria.aplicacion.puertos.IThrottlingService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(4)
@RequiredArgsConstructor
public class ValidadorThrottling implements ValidadorOtp {

    private final IThrottlingService throttlingService;

    @Override
    public void validar(SolicitudGenerarCodigo solicitud) throws RuntimeException {
        String canalThrottling = solicitud.tipo().name().toLowerCase();
        String idThrottling = (solicitud.tipo() == TipoVerificacion.EMAIL)
                ? solicitud.email()
                : solicitud.telefono();
        throttlingService.registrarIntento(canalThrottling, idThrottling);
    }
}
