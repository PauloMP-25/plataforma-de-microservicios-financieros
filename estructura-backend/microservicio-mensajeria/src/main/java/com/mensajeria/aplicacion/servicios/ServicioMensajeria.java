package com.mensajeria.aplicacion.servicios;

import com.mensajeria.aplicacion.dtos.*;
import com.mensajeria.aplicacion.excepciones.UsuarioBloqueadoExcepcion;
import com.mensajeria.dominio.entidades.CodigoVerificacion;
import com.mensajeria.dominio.entidades.CodigoVerificacion.PropositoCodigo;
import com.mensajeria.dominio.entidades.CodigoVerificacion.TipoVerificacion;
import com.mensajeria.dominio.entidades.IntentoValidacion;
import com.mensajeria.dominio.repositorios.CodigoVerificacionRepository;
import com.mensajeria.dominio.repositorios.IntentoValidacionRepository;
import com.mensajeria.infraestructura.clientes.ClienteAuditoria;
import com.mensajeria.infraestructura.clientes.ClienteUsuario;
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
    private final ClienteAuditoria clienteAuditoria;

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String MODULO = "MICROSERVICIO-MENSAJERIA";

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

        // Despacho unificado
        if (solicitud.tipo() == TipoVerificacion.EMAIL) {
            emailService.enviarCodigoOtp(solicitud.email(), codigo, solicitud.proposito());
        } else {
            validarTelefono(solicitud.telefono());
            smsService.enviarCodigoVerificacion(solicitud.telefono(), codigo);
        }

        registrarAuditoria(solicitud.usuarioId(), "OTP_GENERADO",
                "Código de " + solicitud.proposito() + " enviado por " + solicitud.tipo());

        return new RespuestaGeneracion(true, "Enviado con éxito", solicitud.tipo().name());
    }

    // =========================================================================
    // 2. VALIDACIÓN (Registro / Activación)
    // =========================================================================
    @Transactional
    public RespuestaValidacion validarParaActivacion(SolicitudValidarCodigo solicitud) {
        CodigoVerificacion codigo = procesarValidacionInterna(solicitud, PropositoCodigo.ACTIVACION_CUENTA);

        // Nueva lógica: Activación por ID en MS-Usuarios
        try {
            clienteUsuario.activarCuenta(codigo.getUsuarioId());
        } catch (Exception e) {
            log.error("[FEIGN] Error activando cuenta {}: {}", codigo.getUsuarioId(), e.getMessage());
        }

        return new RespuestaValidacion(true, "Cuenta activada correctamente.");
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

    private boolean registrarIntentoFallido(UUID uId) {
        IntentoValidacion i = intentoRepository.findByUsuarioId(uId)
                .orElseGet(() -> IntentoValidacion.builder().usuarioId(uId).build());
        i.incrementarIntentos();
        if (i.getIntentos() >= maxIntentos) {
            i.bloquear(bloqueoHoras);
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

    private void registrarAuditoria(UUID uId, String accion, String desc) {
        clienteAuditoria.enviar(new RegistroAuditoriaDTO(uId.toString(), accion, desc, "INTERNAL", MODULO));
    }
}
