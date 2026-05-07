package com.usuario.infraestructura.clientes;

import com.usuario.aplicacion.dtos.SolicitudGenerarOtp;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 *
 * @author user
 */
@Component
@Slf4j
public class ClienteMensajeriaFallback implements ClienteMensajeria {

    @Override
    public void generarCodigo(SolicitudGenerarOtp solicitud) {
        log.error("Fallo crítico: No se pudo conectar con ms-mensajeria para generar OTP. El mensaje quedará en cola si se usa RabbitMQ.");
    }

    @Override
    public UUID validarCodigoYObtenerUsuario(UUID usuarioId, String codigo) {
        log.error("Fallo crítico: No se puede validar código de recuperación porque ms-mensajeria está offline.");
        return null; // Aquí el servicio de autenticación debe manejar el null
    }

    @Override
    public void validarLimite(SolicitudGenerarOtp solicitud) {
        log.warn("MS-Mensajería offline: Saltando validación de límite de OTP para no bloquear al usuario.");
        // Al ser void, simplemente no hace nada y permite que el flujo continúe
    }

}
