package com.mensajeria.aplicacion.fabricas;

import com.mensajeria.aplicacion.dtos.solicitudes.SolicitudGenerarCodigo;
import com.mensajeria.dominio.entidades.CodigoVerificacion;
import com.mensajeria.infraestructura.configuracion.PropiedadesOtp;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Fábrica centralizada para la creación de la entidad CodigoVerificacion.
 * <p>
 * Implementa el patrón Factory Method, encapsulando las reglas de construcción
 * de la entidad del dominio, aplicando los tiempos de expiración dictados por
 * la configuración del sistema.
 * </p>
 */
@Component
@RequiredArgsConstructor
public class FabricaCodigoVerificacion {

    private final PropiedadesOtp propiedadesOtp;

    /**
     * Construye una nueva instancia de CodigoVerificacion a partir de la solicitud
     * del usuario.
     * 
     * @param solicitud DTO de entrada con la información de canal y propósito.
     * @param codigo    OTP aleatorio previamente generado.
     * @return Entidad lista para persistencia.
     */
    public CodigoVerificacion crear(SolicitudGenerarCodigo solicitud, String codigo) {
        return CodigoVerificacion.builder()
                .usuarioId(solicitud.usuarioId())
                .email(solicitud.email())
                .telefono(solicitud.telefono())
                .codigo(codigo)
                .tipo(solicitud.tipo())
                .proposito(solicitud.proposito())
                .fechaExpiracion(LocalDateTime.now().plusMinutes(propiedadesOtp.getExpiracionMinutos()))
                .build();
    }
}
