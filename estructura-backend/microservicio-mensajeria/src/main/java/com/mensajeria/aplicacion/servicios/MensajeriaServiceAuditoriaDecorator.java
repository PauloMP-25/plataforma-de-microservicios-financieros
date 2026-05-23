package com.mensajeria.aplicacion.servicios;

import com.libreria.comun.enums.PropositoCodigo;
import com.mensajeria.aplicacion.dtos.respuestas.RespuestaGeneracion;
import com.mensajeria.aplicacion.dtos.respuestas.RespuestaValidacion;
import com.mensajeria.aplicacion.dtos.solicitudes.SolicitudGenerarCodigo;
import com.mensajeria.aplicacion.dtos.solicitudes.SolicitudValidarCodigo;
import com.mensajeria.aplicacion.excepciones.UsuarioBloqueadoExcepcion;
import com.mensajeria.aplicacion.puertos.IMensajeriaService;
import com.mensajeria.infraestructura.mensajeria.PublicadorAuditoria;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Decorador para el servicio de mensajería que separa la responsabilidad
 * de auditoría de la lógica de negocio (Patrón Decorator).
 */
@Service
@Primary
@Slf4j
public class MensajeriaServiceAuditoriaDecorator implements IMensajeriaService {

    private final IMensajeriaService servicioReal;
    private final PublicadorAuditoria publicadorAuditoria;

    public MensajeriaServiceAuditoriaDecorator(@Qualifier("mensajeriaServiceImpl") IMensajeriaService servicioReal,
                                               PublicadorAuditoria publicadorAuditoria) {
        this.servicioReal = servicioReal;
        this.publicadorAuditoria = publicadorAuditoria;
    }

    @Override
    public RespuestaGeneracion generarYEnviarCodigo(SolicitudGenerarCodigo solicitud) {
        try {
            RespuestaGeneracion respuesta = servicioReal.generarYEnviarCodigo(solicitud);
            
            String detalleAudit = String.format("OTP solicitado para %s vía %s",
                    solicitud.proposito(), solicitud.tipo());
            if (solicitud.telefono() != null && !solicitud.telefono().isBlank()) {
                detalleAudit += " — número: " + enmascararTelefono(solicitud.telefono());
            }
            publicadorAuditoria.publicarEventoSeguridad(solicitud.usuarioId(), "OTP_SOLICITADO", detalleAudit);
            
            return respuesta;
        } catch (UsuarioBloqueadoExcepcion e) {
            publicadorAuditoria.publicarEventoSeguridad(solicitud.usuarioId(), "USUARIO_BLOQUEADO", "Cuenta suspendida temporalmente por múltiples solicitudes/intentos.");
            throw e;
        } catch (Exception e) {
            publicadorAuditoria.publicarEventoSeguridad(solicitud.usuarioId(), "OTP_ERROR", "Fallo al generar/enviar OTP: " + e.getMessage());
            throw e;
        }
    }

    @Override
    public RespuestaValidacion validarParaActivacion(SolicitudValidarCodigo solicitud) {
        try {
            RespuestaValidacion respuesta = servicioReal.validarParaActivacion(solicitud);
            publicadorAuditoria.publicarEventoSeguridad(solicitud.usuarioId(), "OTP_ACTIVACION_EXITOSO", "Validación completada para activación de cuenta");
            return respuesta;
        } catch (UsuarioBloqueadoExcepcion e) {
            publicadorAuditoria.publicarEventoSeguridad(solicitud.usuarioId(), "USUARIO_BLOQUEADO", "Cuenta suspendida temporalmente por múltiples intentos fallidos.");
            throw e;
        } catch (Exception e) {
            publicadorAuditoria.publicarEventoSeguridad(solicitud.usuarioId(), "OTP_INTENTO_FALLIDO", "Intento fallido de validación: " + e.getMessage());
            throw e;
        }
    }

    @Override
    public UUID validarCodigoYObtenerUsuario(UUID usuarioId, String codigoStr) {
        try {
            UUID userId = servicioReal.validarCodigoYObtenerUsuario(usuarioId, codigoStr);
            publicadorAuditoria.publicarEventoSeguridad(userId, "OTP_RESET_EXITOSO", "Validación completada para cambio de contraseña");
            return userId;
        } catch (UsuarioBloqueadoExcepcion e) {
            publicadorAuditoria.publicarEventoSeguridad(usuarioId, "USUARIO_BLOQUEADO", "Cuenta suspendida temporalmente por múltiples intentos fallidos.");
            throw e;
        } catch (Exception e) {
            publicadorAuditoria.publicarEventoSeguridad(usuarioId, "OTP_INTENTO_FALLIDO", "Intento fallido de validación: " + e.getMessage());
            throw e;
        }
    }

    @Override
    public void verificarRestricciones(UUID usuarioId, PropositoCodigo proposito) {
        servicioReal.verificarRestricciones(usuarioId, proposito);
    }

    /**
     * Delega la búsqueda dinámica directamente al servicio real.
     * No requiere auditoría al ser una operación de solo lectura administrativa.
     */
    @Override
    public org.springframework.data.domain.Page<com.mensajeria.dominio.entidades.CodigoVerificacion> buscarCodigos(
            UUID usuarioId, PropositoCodigo proposito, Boolean usado,
            java.time.LocalDateTime inicio, java.time.LocalDateTime fin,
            org.springframework.data.domain.Pageable pageable) {
        return servicioReal.buscarCodigos(usuarioId, proposito, usado, inicio, fin, pageable);
    }
    
    private String enmascararTelefono(String tel) {
        if (tel == null || tel.length() < 4)
            return "****";
        return "****" + tel.substring(tel.length() - 4);
    }
}
