package com.mensajeria.aplicacion.servicios;

import com.mensajeria.aplicacion.dtos.MensajeriaDtos.*;
import com.mensajeria.aplicacion.excepciones.UsuarioBloqueadoExcepcion;
import com.mensajeria.dominio.entidades.CodigoVerificacion;
import com.mensajeria.dominio.entidades.CodigoVerificacion.TipoVerificacion;
import com.mensajeria.dominio.entidades.IntentoValidacion;
import com.mensajeria.dominio.repositorios.CodigoVerificacionRepository;
import com.mensajeria.dominio.repositorios.IntentoValidacionRepository;
import com.mensajeria.infraestructura.clientes.ClienteAuditoria;
import com.mensajeria.infraestructura.clientes.ClienteUsuario;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Núcleo de lógica de negocio del Microservicio-Mensajería.
 *
 * Responsabilidades: 1. Generar códigos OTP de 6 dígitos asociados a un
 * usuarioId. 2. Despacharlos por EMAIL o PHONE según el tipo solicitado. 3.
 * Validar el código con protección por intentos (bloqueo de 10h tras 3 fallos).
 * 4. En validación exitosa, activar la cuenta vía Feign al
 * Microservicio-Usuario. 5. Reportar cada evento al Microservicio-Auditoría de
 * forma asíncrona. 6. Limpiar códigos expirados cada 24 horas mediante
 * @Scheduled.
 */
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

    // ═══════════════════════════════════════════════════════════════════════════
    // 1. GENERACIÓN DE CÓDIGO OTP
    // ═══════════════════════════════════════════════════════════════════════════
    /**
     * Genera un código OTP de 6 dígitos, lo persiste vinculado al usuarioId y
     * lo despacha por el canal solicitado (EMAIL o PHONE).
     *
     * @param solicitud DTO con usuarioId, email, telefono y tipo
     * @return respuesta con confirmación del envío
     */
    @Transactional
    public RespuestaGeneracion generarYEnviarCodigo(SolicitudGenerarCodigo solicitud) {

        UUID usuarioId = solicitud.usuarioId();
        log.info("[OTP] Generando código — usuarioId: {}, tipo: {}", usuarioId, solicitud.tipo());

        // Verificar que el usuario no esté bloqueado antes de generar
        verificarBloqueo(usuarioId);

        String codigo = generarCodigo();

        CodigoVerificacion entidad = CodigoVerificacion.builder()
                .usuarioId(usuarioId)
                .email(solicitud.email())
                .telefono(solicitud.telefono())
                .codigo(codigo)
                .tipo(solicitud.tipo())
                .fechaExpiracion(LocalDateTime.now().plusMinutes(expiracionMinutos))
                .usado(false)
                .build();

        codigoRepository.save(entidad);

        // Despacho por canal
        despacharCodigo(solicitud, codigo);

        // Auditoría (no bloqueante)
        clienteAuditoria.enviar(new RegistroAuditoriaDTO(
                usuarioId.toString(),
                "OTP_GENERADO",
                String.format("Código OTP generado y enviado por %s al destino %s",
                        solicitud.tipo(),
                        solicitud.tipo() == TipoVerificacion.EMAIL ? solicitud.email() : solicitud.telefono()),
                null,
                MODULO
        ));

        log.info("[OTP] Código generado y enviado — usuarioId: {}, tipo: {}", usuarioId, solicitud.tipo());

        return new RespuestaGeneracion(
                true,
                "Código enviado correctamente. Válido por " + expiracionMinutos + " minutos.",
                solicitud.tipo().name()
        );
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 2. VALIDACIÓN DE CÓDIGO OTP
    // ═══════════════════════════════════════════════════════════════════════════
    /**
     * Valida el código OTP ingresado por el usuario.
     *
     * Flujo: - Si el usuario está bloqueado → lanza UsuarioBloqueadoException.
     * - Si no existe código pendiente → error de negocio. - Si el código es
     * incorrecto → incrementa intentos; al 3er fallo, bloquea 10h. - Si es
     * correcto → activa la cuenta en ms-usuario vía Feign y resetea intentos.
     *
     * @param solicitud DTO con usuarioId y codigo ingresado
     * @param tokenActivacion token del TokenConfirmacionEmail en ms-usuario
     * @return respuesta de validación
     */
    @Transactional
    public RespuestaValidacion validarCodigo(SolicitudValidarCodigo solicitud,
            String tokenActivacion) {
        UUID usuarioId = solicitud.usuarioId();
        log.info("[OTP] Validando código — usuarioId: {}", usuarioId);

        // ── 1. Verificar bloqueo ─────────────────────────────────────────────
        verificarBloqueo(usuarioId);

        // ── 2. Buscar código pendiente más reciente ─────────────────────────
        // Buscamos el más reciente sin importar tipo; si necesitas discriminar por tipo,
        // añade el parámetro tipo al DTO de solicitud y al query del repositorio.
        CodigoVerificacion codigoEntidad = codigoRepository
                .findTopByUsuarioIdAndTipoAndUsadoFalseOrderByFechaCreacionDesc(
                        usuarioId, TipoVerificacion.EMAIL)
                .or(() -> codigoRepository
                .findTopByUsuarioIdAndTipoAndUsadoFalseOrderByFechaCreacionDesc(
                        usuarioId, TipoVerificacion.PHONE))
                .orElseThrow(() -> {
                    log.warn("[OTP] No hay código pendiente para usuarioId: {}", usuarioId);
                    return new IllegalStateException(
                            "No hay un código OTP pendiente para este usuario.");
                });

        // ── 3. Validar ──────────────────────────────────────────────────────
        if (!codigoEntidad.isValido(solicitud.codigo())) {

            boolean bloqueado = registrarIntentoFallido(usuarioId);

            // Auditoría del intento fallido
            clienteAuditoria.enviar(new RegistroAuditoriaDTO(
                    usuarioId.toString(),
                    "OTP_VALIDACION_FALLIDA",
                    codigoEntidad.isExpirado()
                    ? "Código expirado"
                    : "Código incorrecto — usuario " + (bloqueado ? "BLOQUEADO" : "con intentos restantes"),
                    null,
                    MODULO
            ));

            if (bloqueado) {
                long horas = calcularHorasRestantes(usuarioId);
                throw new UsuarioBloqueadoExcepcion(usuarioId, horas);
            }

            if (codigoEntidad.isExpirado()) {
                throw new IllegalStateException("El código OTP ha expirado. Solicite uno nuevo.");
            }

            throw new IllegalArgumentException("Código OTP incorrecto.");
        }

        // ── 4. Marcar código como usado ─────────────────────────────────────
        codigoEntidad.setUsado(true);
        codigoEntidad.setFechaUso(LocalDateTime.now());
        codigoRepository.save(codigoEntidad);

        // ── 5. Resetear intentos del usuario ────────────────────────────────
        resetearIntentos(usuarioId);

        // ── 6. Activar cuenta en Microservicio-Usuario vía Feign ────────────
        activarCuentaEnMsUsuario(usuarioId, tokenActivacion);

        // ── 7. Auditoría del éxito ───────────────────────────────────────────
        clienteAuditoria.enviar(new RegistroAuditoriaDTO(
                usuarioId.toString(),
                "OTP_VALIDACION_EXITOSA",
                "Código OTP validado correctamente. Cuenta activada.",
                null,
                MODULO
        ));

        log.info("[OTP] Validación exitosa — usuarioId: {}", usuarioId);
        return new RespuestaValidacion(true, "Código verificado. Cuenta activada correctamente.");
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 3. LIMPIEZA AUTOMÁTICA — @Scheduled cada 24 horas
    // ═══════════════════════════════════════════════════════════════════════════
    /**
     * Elimina los códigos OTP expirados cada 24 horas. fixedDelay = 86 400 000
     * ms = 24 h. El primer ciclo arranca 24h después del inicio.
     */
    @Scheduled(fixedDelay = 86_400_000, initialDelay = 86_400_000)
    @Transactional
    public void eliminarCodigosExpirados() {
        LocalDateTime ahora = LocalDateTime.now();
        int eliminados = codigoRepository.eliminarCodigosExpirados(ahora);
        log.info("[LIMPIEZA] Códigos OTP expirados eliminados: {}", eliminados);
    }

    /**
     * Desbloquea automáticamente usuarios cuyo bloqueo ya expiró. Corre cada 15
     * minutos.
     */
    @Scheduled(fixedDelay = 900_000)
    @Transactional
    public void desbloquearUsuariosExpirados() {
        int desbloqueados = intentoRepository.desbloquearUsuariosExpirados(LocalDateTime.now());
        if (desbloqueados > 0) {
            log.info("[LIMPIEZA] Usuarios OTP desbloqueados automáticamente: {}", desbloqueados);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // MÉTODOS PRIVADOS
    // ═══════════════════════════════════════════════════════════════════════════
    /**
     * Genera un código de 6 dígitos con SecureRandom.
     */
    private String generarCodigo() {
        int codigo = 100_000 + RANDOM.nextInt(900_000);
        return String.valueOf(codigo);
    }

    /**
     * Despacha el código al canal correcto (EMAIL o PHONE).
     */
    private void despacharCodigo(SolicitudGenerarCodigo solicitud, String codigo) {
        if (solicitud.tipo() == TipoVerificacion.EMAIL) {
            emailService.enviarCodigoVerificacion(solicitud.email(), codigo);
        } else {
            if (solicitud.telefono() == null || solicitud.telefono().isBlank()) {
                throw new IllegalArgumentException(
                        "El campo 'telefono' es obligatorio para el tipo PHONE.");
            }
            if (!smsService.isValidPhoneNumber(solicitud.telefono())) {
                throw new IllegalArgumentException(
                        "Formato de teléfono inválido. Debe incluir código de país (+51...).");
            }
            smsService.enviarCodigoVerificacion(solicitud.telefono(), codigo);
        }
    }

    /**
     * Lanza excepción si el usuarioId está bloqueado y el bloqueo aún está
     * vigente.
     */
    private void verificarBloqueo(UUID usuarioId) {
        intentoRepository.findByUsuarioId(usuarioId).ifPresent(intento -> {
            if (intento.isBloqueado() && !intento.bloqueoExpirado()) {
                long horas = ChronoUnit.HOURS.between(LocalDateTime.now(), intento.getBloqueadoHasta());
                log.warn("[OTP] usuarioId bloqueado: {} — {}h restantes", usuarioId, horas);
                throw new UsuarioBloqueadoExcepcion(usuarioId, horas);
            }
            // Si el bloqueo expiró, lo limpiamos
            if (intento.isBloqueado() && intento.bloqueoExpirado()) {
                intento.reiniciar();
                intentoRepository.save(intento);
            }
        });
    }

    /**
     * Registra un intento de validación fallido.
     *
     * @return true si el usuario fue bloqueado en este intento
     */
    private boolean registrarIntentoFallido(UUID usuarioId) {
        IntentoValidacion intento = intentoRepository.findByUsuarioId(usuarioId)
                .orElseGet(() -> IntentoValidacion.builder()
                .usuarioId(usuarioId)
                .build());

        intento.incrementarIntentos();

        boolean debeBloquear = intento.getIntentos() >= maxIntentos;
        if (debeBloquear) {
            intento.bloquear(bloqueoHoras);
            log.warn("[OTP] usuarioId BLOQUEADO tras {} intentos: {}", maxIntentos, usuarioId);
        }

        intentoRepository.save(intento);
        return debeBloquear;
    }

    /**
     * Resetea el contador de intentos al validar correctamente.
     */
    private void resetearIntentos(UUID usuarioId) {
        intentoRepository.findByUsuarioId(usuarioId).ifPresent(intento -> {
            intento.reiniciar();
            intentoRepository.save(intento);
        });
    }

    /**
     * Calcula las horas restantes de bloqueo para el mensaje de error.
     */
    private long calcularHorasRestantes(UUID usuarioId) {
        return intentoRepository.findByUsuarioId(usuarioId)
                .filter(i -> i.getBloqueadoHasta() != null)
                .map(i -> ChronoUnit.HOURS.between(LocalDateTime.now(), i.getBloqueadoHasta()))
                .orElse(bloqueoHoras);
    }

    /**
     * Llama al Microservicio-Usuario vía Feign para activar la cuenta. Si Feign
     * falla, lo registra pero NO interrumpe la respuesta al cliente, ya que el
     * OTP fue válido. El equipo puede reintentar la activación.
     */
    private void activarCuentaEnMsUsuario(UUID usuarioId, String tokenActivacion) {
        if (tokenActivacion == null || tokenActivacion.isBlank()) {
            log.warn("[FEIGN] tokenActivacion nulo/vacío para usuarioId {}. "
                    + "La cuenta NO será activada automáticamente.", usuarioId);
            return;
        }
        try {
            String respuesta = clienteUsuario.confirmarEmail(tokenActivacion);
            log.info("[FEIGN] Cuenta activada en ms-usuario — usuarioId: {}, respuesta: {}",
                    usuarioId, respuesta);
        } catch (Exception ex) {
            log.error("[FEIGN] Error al activar cuenta en ms-usuario para usuarioId {}: {}",
                    usuarioId, ex.getMessage());
            // No relanzamos: el OTP fue correcto; la activación se puede reintentar
        }
    }

}
