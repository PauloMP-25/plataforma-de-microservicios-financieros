package com.mensajeria.aplicacion.servicios;

import com.libreria.comun.enums.PropositoCodigo;
import com.libreria.comun.enums.TipoVerificacion;
import com.libreria.comun.respuesta.ResultadoApi;
import com.mensajeria.aplicacion.dtos.solicitudes.SolicitudGenerarCodigo;
import com.mensajeria.aplicacion.dtos.solicitudes.SolicitudValidarCodigo;
import com.mensajeria.aplicacion.dtos.respuestas.RespuestaGeneracion;
import com.mensajeria.aplicacion.dtos.respuestas.RespuestaValidacion;
import com.mensajeria.aplicacion.dtos.respuestas.RespuestaCodigoAuditoria;
import com.mensajeria.aplicacion.excepciones.*;
import com.mensajeria.dominio.entidades.CodigoVerificacion;
import com.mensajeria.dominio.entidades.IntentoValidacion;
import com.mensajeria.dominio.especificaciones.MensajeriaSpecs;
import com.mensajeria.dominio.repositorios.CodigoVerificacionRepository;
import com.mensajeria.dominio.repositorios.IntentoValidacionRepository;
import com.mensajeria.aplicacion.puertos.IMensajeriaService;
import com.mensajeria.aplicacion.servicios.canales.NotificacionService;
import com.mensajeria.aplicacion.servicios.canales.TipoNotificacion;
import java.util.Map;
import com.mensajeria.infraestructura.clientes.ClienteUsuario;
import com.mensajeria.infraestructura.clientes.ClienteActualizarTelefono;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Implementación principal del servicio de mensajería y OTP de LUKA APP.
 * <p>
 * Orquesta la generación y validación de códigos de un solo uso, diferenciando
 * los flujos de {@code ACTIVACION_CUENTA} y {@code RESTABLECER_PASSWORD}.
 * Integra throttling por canal (Redis), auditoría (RabbitMQ) y sincronización
 * con el ms-usuario mediante Feign con fallback de Resilience4j.
 * </p>
 *
 * @author Paulo Moron
 * @version 1.1.0
 */
@Service("mensajeriaServiceImpl")
@Slf4j
@RequiredArgsConstructor
public class MensajeriaServiceImpl implements IMensajeriaService {

    private final CodigoVerificacionRepository codigoRepository;
    private final IntentoValidacionRepository intentoRepository;
    private final NotificacionService notificacionService;
    private final ClienteUsuario clienteUsuario;
    private final ClienteActualizarTelefono usuarioFeignClient;

    private static final SecureRandom RANDOM = new SecureRandom();

    private final com.mensajeria.infraestructura.configuracion.PropiedadesOtp propiedadesOtp;
    private final java.util.List<com.mensajeria.aplicacion.servicios.validadores.ValidadorOtp> validadores;
    private final com.mensajeria.aplicacion.fabricas.FabricaCodigoVerificacion fabricaCodigo;
    private final com.mensajeria.infraestructura.configuracion.PropiedadesTwilio propiedadesTwilio;

    // =========================================================================
    // 1. GENERACIÓN Y ENVÍO
    // =========================================================================

    /**
     * {@inheritDoc}
     * <p>
     * Flujo interno:
     * <ol>
     *   <li>Valida que el usuario no esté bloqueado por intentos fallidos.</li>
     *   <li>Valida que no haya superado el límite diario de 3 códigos.</li>
     *   <li>Registra el intento de throttling por canal en Redis.</li>
     *   <li>Genera un código OTP aleatorio de 6 dígitos y lo persiste.</li>
     *   <li>Despacha el código por EMAIL o SMS según el canal de la solicitud.</li>
     *   <li>Publica un evento de auditoría asíncrono vía RabbitMQ.</li>
     * </ol>
     * </p>
     */
    @Override
    @Transactional
    public RespuestaGeneracion generarYEnviarCodigo(SolicitudGenerarCodigo solicitud) {
        // Ejecutar cadena de responsabilidad para validaciones
        validadores.forEach(v -> v.validar(solicitud));

        String codigo = String.valueOf(100_000 + RANDOM.nextInt(900_000));

        CodigoVerificacion entidad = fabricaCodigo.crear(solicitud, codigo);
        codigoRepository.save(entidad);

        Map<String, Object> variables = Map.of(
                "codigo", codigo,
                "proposito", solicitud.proposito()
        );

        // Resolvemos el canal y el destino
        TipoNotificacion tipoEnvio = switch (solicitud.tipo()) {
            case EMAIL -> TipoNotificacion.EMAIL;
            case SMS -> TipoNotificacion.SMS;
            case WHATSAPP -> TipoNotificacion.WHATSAPP;
        };

        String destino = (solicitud.tipo() == TipoVerificacion.EMAIL) 
                ? solicitud.email() 
                : solicitud.telefono();

        // Enviamos de forma agnóstica (la implementación decide si es SMTP o Twilio)
        notificacionService.enviar(tipoEnvio, destino, variables);



        return new RespuestaGeneracion(true, "Código enviado exitosamente", solicitud.tipo());
    }

    // =========================================================================
    // 2. VALIDACIÓN — ACTIVACIÓN DE CUENTA
    // =========================================================================

    /**
     * {@inheritDoc}
     * <p>
     * Si el OTP es correcto, notifica al ms-usuario para activar la cuenta y
     * sincroniza el teléfono si el canal fue SMS.
     * </p>
     */
    @Override
    @Transactional(noRollbackFor = {CodigoInvalidoException.class, CodigoExpiradoException.class, UsuarioBloqueadoExcepcion.class})
    public RespuestaValidacion validarParaActivacion(SolicitudValidarCodigo solicitud) {
        CodigoVerificacion cv = procesarValidacionInterna(solicitud, PropositoCodigo.ACTIVACION_CUENTA);
        String telefonoVerificado = cv.getTelefono();

        log.info("[MS-MENSAJERIA] Activando cuenta para usuario: {} con teléfono: {}",
                cv.getUsuarioId(), telefonoVerificado);

        com.libreria.comun.respuesta.ResultadoApi<String> resultado = clienteUsuario.activarCuenta(cv.getUsuarioId(), telefonoVerificado);

        if (resultado == null || "ACTIVACION_PENDIENTE".equals(resultado.datos())) {
            log.error("[MS-MENSAJERIA] Activación fallida en ms-usuario para: {}. El OTP sigue siendo válido.", cv.getUsuarioId());
            throw new MensajeriaExternaException("No se pudo activar la cuenta en el servicio de usuario. El OTP sigue activo. Intente de nuevo.", "ms-usuario offline durante la activación");
        }

        // Mover el cv.setUsado(true) a después de que activarCuenta() confirme éxito — no antes.
        cv.setUsado(true);
        cv.setFechaUso(LocalDateTime.now());
        reiniciarIntentos(cv.getUsuarioId());
        codigoRepository.save(cv);

        return new RespuestaValidacion(true, "OTP válido. Cuenta activada y teléfono sincronizado.");
    }

    // =========================================================================
    // 3. VALIDACIÓN — RECUPERACIÓN DE CONTRASEÑA
    // =========================================================================

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(noRollbackFor = {CodigoInvalidoException.class, CodigoExpiradoException.class, UsuarioBloqueadoExcepcion.class})
    public UUID validarCodigoYObtenerUsuario(UUID usuarioId, String codigoStr) {
        CodigoVerificacion cv = procesarValidacionInterna(
                new SolicitudValidarCodigo(usuarioId, codigoStr),
                PropositoCodigo.RESTABLECER_PASSWORD);

        cv.setUsado(true);
        cv.setFechaUso(LocalDateTime.now());
        reiniciarIntentos(cv.getUsuarioId());
        codigoRepository.save(cv);

        // Sincronizar el teléfono verificado tras la validación exitosa del OTP
        if (cv.getTipo() == TipoVerificacion.SMS || cv.getTipo() == TipoVerificacion.WHATSAPP) {
            String telefonoVerificado = cv.getTelefono();
            if (telefonoVerificado != null && !telefonoVerificado.isBlank()) {
                try {
                    log.info("[MS-MENSAJERIA] Sincronizando teléfono verificado tras validación OTP exitosa.");
                    ResultadoApi<String> resultado = usuarioFeignClient.sincronizarTelefono(
                            cv.getUsuarioId(), telefonoVerificado);
                    if (resultado != null && "SINCRONIZACION_PENDIENTE".equals(resultado.datos())) {
                        log.warn("[FEIGN] Sincronización de teléfono pendiente en ms-usuario para: {}",
                                cv.getUsuarioId());
                    }
                } catch (Exception e) {
                    log.error("[FEIGN] Error al sincronizar el teléfono con ms-usuario. Fallback no disponible o error interno. Se continuará con el flujo.", e);
                }
            }
        }

        return cv.getUsuarioId();
    }

    // =========================================================================
    // 4. VERIFICACIÓN ANTICIPADA DE RESTRICCIONES
    // =========================================================================

    /**
     * {@inheritDoc}
     */
    @Override
    public void verificarRestricciones(UUID usuarioId, PropositoCodigo proposito) {
        verificarBloqueo(usuarioId);
        verificarLimiteDiario(usuarioId, proposito);
        log.info("[MS-MENSAJERIA] Restricciones OK para usuario: {}", usuarioId);
    }

    // =========================================================================
    // LÓGICA PRIVADA COMPARTIDA
    // =========================================================================

    /**
     * Valida el OTP internamente para el propósito dado, registrando intentos
     * fallidos y bloqueando al usuario si supera el máximo configurado.
     *
     * @param sol  DTO con el ID del usuario y el código ingresado.
     * @param prop Propósito esperado del OTP ({@code ACTIVACION_CUENTA} o
     *             {@code RECUPERACION_PASSWORD}).
     * @return Entidad {@link CodigoVerificacion} validada (no guardada como usada).
     * @throws UsuarioBloqueadoExcepcion si el usuario ya está bloqueado.
     * @throws CodigoPendienteNotFoundException si no hay códigos pendientes.
     * @throws CodigoExpiradoException si el código ya expiró.
     * @throws CodigoInvalidoException si el código es incorrecto.
     */
    private CodigoVerificacion procesarValidacionInterna(SolicitudValidarCodigo sol, PropositoCodigo prop) {
        verificarBloqueo(sol.usuarioId());

        CodigoVerificacion cv = codigoRepository
                .findTopByUsuarioIdAndPropositoAndUsadoFalseOrderByFechaCreacionDesc(sol.usuarioId(), prop)
                .orElseThrow(() -> new CodigoPendienteNotFoundException(sol.usuarioId()));

        if (cv.isExpirado()) {
            if (registrarIntentoFallido(sol.usuarioId())) {
                throw new UsuarioBloqueadoExcepcion(sol.usuarioId(), propiedadesOtp.getBloqueoHoras());
            }
            throw new CodigoExpiradoException();
        }

        if (!cv.getCodigo().equals(sol.codigo())) {
            if (registrarIntentoFallido(sol.usuarioId())) {
                throw new UsuarioBloqueadoExcepcion(sol.usuarioId(), propiedadesOtp.getBloqueoHoras());
            }
            throw new CodigoInvalidoException("código incorrecto");
        }

        return cv;
    }

    /**
     * Verifica si el usuario tiene un bloqueo activo por intentos fallidos previos.
     *
     * @param uId UUID del usuario a verificar.
     * @throws UsuarioBloqueadoExcepcion si el bloqueo aún no ha expirado.
     */
    private void verificarBloqueo(UUID uId) {
        intentoRepository.findByUsuarioId(uId).ifPresent(i -> {
            if (i.isBloqueado() && !i.bloqueoExpirado()) {
                throw new UsuarioBloqueadoExcepcion(uId,
                        ChronoUnit.HOURS.between(LocalDateTime.now(), i.getBloqueadoHasta()));
            }
        });
    }

    /**
     * Incrementa el contador de intentos fallidos y bloquea al usuario si supera
     * el máximo. Emite advertencia de auditoría en el segundo intento.
     *
     * @param uId UUID del usuario que falló la validación.
     * @return {@code true} si el usuario quedó bloqueado tras este intento.
     */
    private boolean registrarIntentoFallido(UUID uId) {
        IntentoValidacion i = intentoRepository.findByUsuarioId(uId)
                .orElseGet(() -> IntentoValidacion.builder().usuarioId(uId).build());

        i.incrementarIntentos();
        int intentosActuales = i.getIntentos();

        if (intentosActuales >= propiedadesOtp.getMaxIntentos()) {
            i.bloquear(propiedadesOtp.getBloqueoHoras());
        }

        intentoRepository.save(i);
        return i.isBloqueado();
    }

    /**
     * Reinicia el contador de intentos fallidos del usuario tras una validación
     * exitosa, eliminando cualquier bloqueo activo.
     *
     * @param uId UUID del usuario cuyo registro de intentos debe reiniciarse.
     */
    @org.springframework.cache.annotation.CacheEvict(value="bloqueos-otp", key="#uId")
    public void reiniciarIntentos(UUID uId) {
        intentoRepository.findByUsuarioId(uId).ifPresent(i -> {
            i.reiniciar();
            intentoRepository.save(i);
        });
    }

    /**
     * Verifica que el usuario no haya superado el límite de 3 solicitudes diarias
     * para el propósito dado, contando desde el inicio del día actual.
     *
     * @param uId       UUID del usuario a verificar.
     * @param proposito Propósito del OTP para contar solo las solicitudes del mismo
     *                  tipo.
     * @throws LimiteCodigosExcedidoException si ya agotó los 3 intentos diarios.
     */
    private void verificarLimiteDiario(UUID uId, PropositoCodigo proposito) {
        LocalDateTime inicioDia = LocalDateTime.now().toLocalDate().atStartOfDay();
        long pedidosHoy = codigoRepository.countByUsuarioIdAndPropositoAndFechaCreacionAfter(
                uId, proposito, inicioDia);

        if (pedidosHoy >= 3) {
            log.warn("[MS-MENSAJERIA] Límite diario alcanzado — usuario: {}, propósito: {}", uId, proposito);
            throw new LimiteCodigosExcedidoException(
                    "Has alcanzado el límite de 3 solicitudes diarias para este trámite. Inténtalo mañana.");
        }
    }

    // =========================================================================
    // 5. BÚSQUEDA DINÁMICA — SPECIFICATION PATTERN (Auditoría)
    // =========================================================================

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public Page<RespuestaCodigoAuditoria> buscarCodigos(UUID usuarioId, PropositoCodigo proposito,
            Boolean usado, LocalDateTime inicio,
            LocalDateTime fin, Pageable pageable) {
        Specification<CodigoVerificacion> spec = Specification.where(MensajeriaSpecs.porUsuario(usuarioId))
                .and(MensajeriaSpecs.porProposito(proposito))
                .and(MensajeriaSpecs.estaUsado(usado))
                .and(MensajeriaSpecs.creadoEntre(inicio, fin));

        return codigoRepository.findAll(spec, pageable)
                .map(cv -> new RespuestaCodigoAuditoria(
                        cv.getId(), cv.getUsuarioId(), cv.getEmail(), cv.getTelefono(),
                        cv.getTipo(), cv.getProposito(), cv.getFechaCreacion(),
                        cv.getFechaExpiracion(), cv.getUsado(), cv.getFechaUso()
                ));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean validarConexionTwilio() {
        try {
            // Validación puramente local (no realiza llamadas a la API de Twilio)
            // Evita errores de permisos (ej. falta de twilio/iam/accounts/read en API Keys Restringidas)
            // y no bloquea el arranque con peticiones de red pesadas en el Health Check.
            String accountSid = propiedadesTwilio.getAccountSid() != null ? propiedadesTwilio.getAccountSid() : propiedadesTwilio.getAccount().getSid();
            String apiKeySid = propiedadesTwilio.getApiKeySid() != null ? propiedadesTwilio.getApiKeySid() : propiedadesTwilio.getApiKey().getSid();
            
            if (accountSid == null || accountSid.isBlank()) {
                throw new IllegalStateException("El Account SID de Twilio no está configurado.");
            }
            
            boolean tieneApiKey = apiKeySid != null && !apiKeySid.isBlank();
            boolean tieneAuthToken = propiedadesTwilio.getAuth().getToken() != null && !propiedadesTwilio.getAuth().getToken().isBlank();

            if (!tieneApiKey && !tieneAuthToken) {
                throw new IllegalStateException("Falta configurar credenciales de Twilio (API Key o Auth Token).");
            }
            
            // log.trace("[TWILIO-HEALTH] Validación local exitosa. Credenciales presentes para Account SID: {}", accountSid);
            return true;
        } catch (Exception e) {
            log.error("[TWILIO-HEALTH] Error en validación local de Twilio: {}", e.getMessage());
            throw new RuntimeException("Fallo de configuración local en Twilio: " + e.getMessage(), e);
        }
    }
}