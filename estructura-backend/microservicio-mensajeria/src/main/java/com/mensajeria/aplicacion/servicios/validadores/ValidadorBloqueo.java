package com.mensajeria.aplicacion.servicios.validadores;

import com.mensajeria.aplicacion.dtos.solicitudes.SolicitudGenerarCodigo;
import com.mensajeria.aplicacion.excepciones.UsuarioBloqueadoExcepcion;
import com.mensajeria.dominio.repositorios.IntentoValidacionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Component
@Order(2)
@RequiredArgsConstructor
public class ValidadorBloqueo implements ValidadorOtp {

    private final IntentoValidacionRepository intentoRepository;

    @Override
    public void validar(SolicitudGenerarCodigo solicitud) throws RuntimeException {
        intentoRepository.findByUsuarioId(solicitud.usuarioId()).ifPresent(i -> {
            if (i.isBloqueado() && !i.bloqueoExpirado()) {
                throw new UsuarioBloqueadoExcepcion(solicitud.usuarioId(),
                        ChronoUnit.HOURS.between(LocalDateTime.now(), i.getBloqueadoHasta()));
            }
        });
    }
}
