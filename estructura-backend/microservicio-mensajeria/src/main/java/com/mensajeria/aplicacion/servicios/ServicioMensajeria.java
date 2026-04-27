package com.mensajeria.aplicacion.servicios;

import com.mensajeria.aplicacion.dtos.*;
import com.mensajeria.aplicacion.excepciones.LimiteCodigosExcedidoException;
import com.mensajeria.aplicacion.excepciones.UsuarioBloqueadoExcepcion;
import com.mensajeria.dominio.entidades.CodigoVerificacion;
import com.mensajeria.dominio.entidades.CodigoVerificacion.PropositoCodigo;
import com.mensajeria.dominio.entidades.CodigoVerificacion.TipoVerificacion;
import com.mensajeria.dominio.entidades.IntentoValidacion;
import com.mensajeria.dominio.repositorios.CodigoVerificacionRepository;
import com.mensajeria.dominio.repositorios.IntentoValidacionRepository;
import com.mensajeria.infraestructura.clientes.ClienteUsuario;
import com.mensajeria.infraestructura.mensajeria.PublicadorAuditoria;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class ServicioMensajeria {

    private final CodigoVerificacionRepository codigoRepository;
    private final IntentoValidacionRepository intentoRepository;
    private final EmailService emailService;
    private final SmsService smsService;
    private final ClienteUsuario clienteUsuario;
    private final PublicadorAuditoria publicadorAuditoria;

    private static final SecureRandom RANDOM = new SecureRandom();

    @Value("${mensajeria.otp.expiracion-minutos:10}")
    private int expiracionMinutos;

    @Value("${mensajeria.otp.max-intentos:3}")
    private int maxIntentos;

    @Value("${mensajeria.otp.bloqueo-horas:10}")
    private long bloqueoHoras;

    // =========================================================================
    // 1. GENERACIÓN Y ENVÍO
    // =========================================================================
    @Transactional
    public RespuestaGeneracion generarYEnviarCodigo(SolicitudGenerarCodigo solicitud) {
        verificarBloqueo(solicitud.usuarioId());

        verificarLimiteDiario(solicitud.usuarioId(), solicitud.proposito());

        String codigo = String.valueOf(100_000 + RANDOM.nextInt(900_000));

        CodigoVerificacion entidad = CodigoVerificacion.builder()
                .usuarioId(solicitud.usuarioId())
                .email(solicitud.email())
                .telefono(solicitud.telefono())
                .codigo(codigo)
                .tipo(solicitud.tipo())
                .proposito(solicitud.proposito())
                .fechaExpiracion(LocalDateTime.now().plusMinutes(expiracionMinutos))
                .build();

        codigoRepository.save(entidad);

        // Lógica de auditoría informativa según el propósito y canal
        String accionAudit = "OTP_SOLICITADO";
        String detalleAudit = String.format("Usuario solicitó código para %s vía %s",
                solicitud.proposito(), solicitud.tipo());

        if (solicitud.tipo() == TipoVerificacion.EMAIL) {
            emailService.enviarCodigoOtp(solicitud.email(), codigo, solicitud.proposito());
        } else {
            validarTelefono(solicitud.telefono());
            smsService.enviarCodigoVerificacion(solicitud.telefono(), codigo);
            detalleAudit += " al número: " + enmascararTelefono(solicitud.telefono());
        }

        // Registro de auditoría específico
        registrarAuditoria(solicitud.usuarioId(), accionAudit, detalleAudit);
        return new RespuestaGeneracion(true, "Enviado con éxito", solicitud.tipo().name());
    }

    // =========================================================================
    // 2. VALIDACIÓN (Registro / Activación)
    // =========================================================================
    @Transactional
    public RespuestaValidacion validarParaActivacion(SolicitudValidarCodigo solicitud) {
//      1. Validamos que el OTP sea correcto en nuestra base de datos (MS-Mensajería)
        CodigoVerificacion codigo = procesarValidacionInterna(solicitud, PropositoCodigo.ACTIVACION_CUENTA);

        // 2. Si el código es válido, llamamos al MS-Usuarios para que cambie el estado a TRUE
        log.info("[MS-MENSAJERIA] Solicitando activación al MS-Usuarios para el ID: {}", codigo.getUsuarioId());

        clienteUsuario.activarCuenta(codigo.getUsuarioId());

        return new RespuestaValidacion(true, "OTP válido. Cuenta activada en el sistema de usuarios.");
    }

    // =========================================================================
    // 3. VALIDACIÓN (Recuperación / Reset) - Usado por MS-Usuarios
    // =========================================================================
    @Transactional
    public UUID validarCodigoYObtenerUsuario(String codigoStr) {
        // Buscamos el código globalmente (el usuario no está logueado aún)
        CodigoVerificacion cv = codigoRepository.findTopByCodigoAndUsadoFalseOrderByFechaCreacionDesc(codigoStr)
                .orElseThrow(() -> new IllegalArgumentException("Código inválido o ya utilizado."));

        if (cv.isExpirado()) {
            throw new IllegalStateException("El código ha expirado.");
        }
        if (cv.getProposito() != PropositoCodigo.RESTABLECER_PASSWORD) {
            throw new IllegalArgumentException("Código no autorizado para este proceso.");
        }

        cv.setUsado(true);
        cv.setFechaUso(LocalDateTime.now());
        resetearIntentos(cv.getUsuarioId());

        registrarAuditoria(cv.getUsuarioId(), "OTP_RESET_EXITOSO", "Validación completa para cambio de clave");
        return cv.getUsuarioId();
    }

    // =========================================================================
    // LÓGICA PRIVADA COMPARTIDA
    // =========================================================================
    private CodigoVerificacion procesarValidacionInterna(SolicitudValidarCodigo sol, PropositoCodigo prop) {
        verificarBloqueo(sol.usuarioId());

        CodigoVerificacion cv = codigoRepository
                .findTopByUsuarioIdAndPropositoAndUsadoFalseOrderByFechaCreacionDesc(sol.usuarioId(), prop)
                .orElseThrow(() -> new IllegalStateException("No hay códigos pendientes."));

        if (!cv.esValidoPara(sol.codigo(), prop)) {
            if (registrarIntentoFallido(sol.usuarioId())) {
                throw new UsuarioBloqueadoExcepcion(sol.usuarioId(), bloqueoHoras);
            }
            throw new IllegalArgumentException("Código incorrecto o expirado.");
        }

        cv.setUsado(true);
        cv.setFechaUso(LocalDateTime.now());
        resetearIntentos(sol.usuarioId());
        return codigoRepository.save(cv);
    }

    private void verificarBloqueo(UUID uId) {
        intentoRepository.findByUsuarioId(uId).ifPresent(i -> {
            if (i.isBloqueado() && !i.bloqueoExpirado()) {
                throw new UsuarioBloqueadoExcepcion(uId, ChronoUnit.HOURS.between(LocalDateTime.now(), i.getBloqueadoHasta()));
            }
        });
    }

// =========================================================================
    // LÓGICA DE INTENTOS (Alerta de 3er Intento)
    // =========================================================================
    private boolean registrarIntentoFallido(UUID uId) {
        IntentoValidacion i = intentoRepository.findByUsuarioId(uId)
                .orElseGet(() -> IntentoValidacion.builder().usuarioId(uId).build());

        i.incrementarIntentos();
        int intentosActuales = i.getIntentos();

        if (intentosActuales == 2) { // El siguiente (3ero) es el crítico antes del bloqueo
            registrarAuditoria(uId, "OTP_ADVERTENCIA",
                    "Segundo intento fallido. El próximo error bloqueará la cuenta por " + bloqueoHoras + " horas.");
        }

        if (intentosActuales >= maxIntentos) {
            i.bloquear(bloqueoHoras);
            registrarAuditoria(uId, "USUARIO_BLOQUEADO",
                    "Máximo de intentos alcanzado (" + maxIntentos + "). Cuenta suspendida temporalmente.");
        }

        intentoRepository.save(i);
        return i.isBloqueado();
    }

    private void resetearIntentos(UUID uId) {
        intentoRepository.findByUsuarioId(uId).ifPresent(i -> {
            i.reiniciar();
            intentoRepository.save(i);
        });
    }

    private void validarTelefono(String tel) {
        if (tel == null || tel.isBlank()) {
            throw new IllegalArgumentException("Teléfono requerido para SMS.");
        }
        if (!smsService.isValidPhoneNumber(tel)) {
            throw new IllegalArgumentException("Formato +51... requerido.");
        }
    }

    // =========================================================================
    // LÓGICA DE CONTROL DE SPAM
    // =========================================================================
    private void verificarLimiteDiario(UUID uId, PropositoCodigo proposito) {
        // Calculamos el inicio del día actual (00:00:00)
        LocalDateTime inicioDia = LocalDateTime.now().toLocalDate().atStartOfDay();

        // Contamos cuántos códigos ha pedido hoy para ese propósito
        long pedidosHoy = codigoRepository.countByUsuarioIdAndPropositoAndFechaCreacionAfter(
                uId, proposito, inicioDia);

        if (pedidosHoy >= 3) {
            log.warn("[MS-MENSAJERIA] Límite diario alcanzado para usuario: {} en {}", uId, proposito);
            throw new LimiteCodigosExcedidoException(
                    "Has alcanzado el límite de 3 códigos diarios para este trámite. Inténtalo mañana.");
        }
    }

    // =========================================================================
    // HELPERS DE FORMATEO
    // =========================================================================
    private String enmascararTelefono(String tel) {
        if (tel == null || tel.length() < 4) {
            return "****";
        }
        return "****" + tel.substring(tel.length() - 4);
    }

    private void registrarAuditoria(UUID uId, String accion, String desc) {
        publicadorAuditoria.publicarEvento(uId, accion, desc);
    }
}
