package com.usuario.infraestructura.clientes;

import com.usuario.aplicacion.dtos.solicitudes.SolicitudGenerarOtp;
import com.usuario.aplicacion.dtos.solicitudes.SolicitudValidarRecuperacion;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Fallback para el cliente Feign de Mensajería.
 *
 * @author user
 */
@Component
@Slf4j
public class ClienteMensajeriaFallback implements ClienteMensajeria {

    @Override
    public UUID generarCodigo(SolicitudGenerarOtp solicitud) {
        log.error("Fallo crítico: No se pudo conectar con ms-mensajeria para generar OTP. El mensaje quedará en cola si se usa RabbitMQ.");
        return null;
    }

    @Override
    public UUID validarCodigoYObtenerUsuario(SolicitudValidarRecuperacion solicitud) {
        log.error("Fallo crítico: No se puede validar código de recuperación porque ms-mensajeria está offline.");
        return null; // Aquí el servicio de autenticación debe manejar el null
    }

    @Override
    public com.libreria.comun.respuesta.ResultadoApi<com.usuario.aplicacion.dtos.respuestas.RespuestaValidacion> validarActivacion(com.usuario.aplicacion.dtos.solicitudes.SolicitudValidarCodigo solicitud) {
        log.error("Fallo crítico: No se puede validar código de activación porque ms-mensajeria está offline.");
        return null;
    }

    @Override
    public void validarLimite(com.usuario.aplicacion.dtos.solicitudes.SolicitudVerificarLimite solicitud) {
        log.warn("MS-Mensajería offline: Saltando validación de límite de OTP para no bloquear al usuario.");
        // Al ser void, simplemente no hace nada y permite que el flujo continúe
    }
}
