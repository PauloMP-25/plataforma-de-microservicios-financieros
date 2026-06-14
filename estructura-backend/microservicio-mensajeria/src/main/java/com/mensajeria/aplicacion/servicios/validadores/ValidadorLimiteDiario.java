package com.mensajeria.aplicacion.servicios.validadores;

import com.mensajeria.aplicacion.dtos.solicitudes.SolicitudGenerarCodigo;
import com.mensajeria.aplicacion.excepciones.LimiteCodigosExcedidoException;
import com.mensajeria.dominio.repositorios.CodigoVerificacionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@Order(3)
@RequiredArgsConstructor
@Slf4j
public class ValidadorLimiteDiario implements ValidadorOtp {

    private final CodigoVerificacionRepository codigoRepository;

    @Override
    public void validar(SolicitudGenerarCodigo solicitud) throws RuntimeException {
        LocalDateTime inicioDia = LocalDateTime.now().toLocalDate().atStartOfDay();
        long pedidosHoy = codigoRepository.countByUsuarioIdAndPropositoAndFechaCreacionAfter(
                solicitud.usuarioId(), solicitud.proposito(), inicioDia);

        if (pedidosHoy >= 3) {
            log.warn("[MS-MENSAJERIA] Límite diario alcanzado — usuario: {}, propósito: {}", solicitud.usuarioId(), solicitud.proposito());
            throw new LimiteCodigosExcedidoException(
                    "Has alcanzado el límite de 3 solicitudes diarias para este trámite. Inténtalo mañana.");
        }
    }
}
