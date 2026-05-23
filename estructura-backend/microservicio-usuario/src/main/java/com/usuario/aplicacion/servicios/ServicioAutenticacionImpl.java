package com.usuario.aplicacion.servicios;

import com.libreria.comun.enums.EstadoEvento;
import com.libreria.comun.excepciones.ExcepcionNoAutorizado;
import com.libreria.comun.seguridad.ServicioJwt;
import com.usuario.aplicacion.dtos.respuestas.RespuestaAutenticacion;
import com.usuario.aplicacion.dtos.solicitudes.SolicitudCambioPassword;
import com.usuario.aplicacion.dtos.solicitudes.SolicitudLogin;
import com.usuario.aplicacion.dtos.solicitudes.SolicitudRecuperacion;
import com.usuario.aplicacion.dtos.solicitudes.SolicitudRefreshToken;
import com.usuario.aplicacion.dtos.solicitudes.SolicitudRegistro;
import com.usuario.aplicacion.dtos.solicitudes.SolicitudRestablecerPassword;
import com.usuario.aplicacion.dtos.solicitudes.SolicitudGenerarOtp;
import com.usuario.aplicacion.dtos.solicitudes.SolicitudValidarRecuperacion;
import com.usuario.aplicacion.fabricas.FabricaSolicitudOtp;
import com.libreria.comun.enums.TipoVerificacion;
import com.usuario.aplicacion.excepciones.ContrasenasNoCoincidenException;
import com.usuario.aplicacion.excepciones.CredencialesInvalidasException;
import com.usuario.aplicacion.excepciones.CuentaNoHabilitadaException;
import com.usuario.aplicacion.excepciones.TokenInvalidoException;
import com.usuario.aplicacion.excepciones.UsuarioNoEncontradoException;
import com.usuario.aplicacion.excepciones.UsuarioYaExisteException;
import com.usuario.aplicacion.puertos.IServicioAutenticacion;
import com.usuario.aplicacion.puertos.IServicioPerfilCache;
import com.usuario.dominio.entidades.Rol;
import com.usuario.dominio.entidades.Usuario;
import com.usuario.dominio.repositorios.RolRepository;
import com.usuario.dominio.repositorios.UsuarioRepository;
import com.usuario.infraestructura.clientes.ClienteMensajeria;
import com.usuario.infraestructura.clientes.ClientePerfilExterno;
import com.usuario.infraestructura.mensajeria.PublicadorAuditoria;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import java.util.concurrent.TimeUnit;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;
import java.util.HashMap;

/**
 * Implementación del servicio de autenticación.
 * Gestiona el ciclo de vida de seguridad del usuario, incluyendo registro,
 * login, recuperación de contraseña y auditoría.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ServicioAutenticacionImpl implements IServicioAutenticacion {

    private final AuthenticationManager authenticationManager;
    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final ServicioJwt jwtService;
    private final PasswordEncoder passwordEncoder;
    private final ClientePerfilExterno clientePerfilExterno;
    private final IServicioPerfilCache servicioPerfilCache;
    private final ClienteMensajeria clienteMensajeria;
    private final PublicadorAuditoria publicadorAuditoria;
    private final StringRedisTemplate redisTemplate;

    @SuppressWarnings("null")
    @Override
    @Transactional
    public void activarCuenta(String correo, String codigoOtp, String telefono, String ipCliente) {
        Usuario usuario = usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado con correo: " + correo));
        
        UUID usuarioId = usuario.getId();

        if (usuario.isHabilitado()) {
            throw new IllegalArgumentException("La cuenta ya se encuentra activada.");
        }

        // 1. Validar OTP vía microservicio de mensajería (Solo si viene el código)
        if (codigoOtp != null && !codigoOtp.isBlank()) {
            var respuesta = clienteMensajeria.validarActivacion(new com.usuario.aplicacion.dtos.solicitudes.SolicitudValidarCodigo(usuarioId, codigoOtp));
            if (respuesta == null || !respuesta.exito() || !respuesta.datos().esValido()) {
                publicadorAuditoria.publicarAcceso(usuarioId, ipCliente, EstadoEvento.FALLO,
                        "ACTIVACION_FALLIDA_OTP_INVALIDO");
                throw new TokenInvalidoException("Código de activación inválido o expirado.");
            }
        } else {
            log.info("[AUTH-ACT] Activación directa solicitada para usuario: {} (OTP ya validado o no requerido)",
                    usuarioId);
        }

        // 2. Sincronizar teléfono si se proporcionó
        if (telefono != null && !telefono.isBlank()) {
            try {
                clientePerfilExterno.actualizarTelefono(usuarioId, telefono);
            } catch (Exception e) {
                log.error("Error sincronizando teléfono con MS-CLIENTE: {}", e.getMessage());
            }
        }

        // 3. Habilitar cuenta
        usuario.setHabilitado(true);
        usuario.setCuentaNoBloqueada(true);
        usuarioRepository.save(usuario);

        // 4. Crear perfil inicial en MS-CLIENTE (Garantizar consistencia)
        try {
            clientePerfilExterno.crearPerfilInicial(usuarioId);
            log.info("[AUTH-ACT] Perfil inicial creado exitosamente en ms-cliente para usuario: {}", usuarioId);
        } catch (Exception e) {
            log.warn(
                    "[AUTH-ACT] No se pudo crear el perfil inicial para el usuario {}. Se deberá sincronizar posteriormente.",
                    usuarioId);
        }

        // 5. Registrar acceso exitoso
        publicadorAuditoria.publicarAcceso(usuarioId, ipCliente, EstadoEvento.EXITO, "ACTIVACION_CUENTA");
    }

    @Override
    @Transactional
    public RespuestaAutenticacion login(SolicitudLogin request, String ipCliente) {
        Usuario usuario = usuarioRepository.findByCorreo(request.correo())
                .orElseThrow(() -> new UsuarioNoEncontradoException("Usuario no encontrado: " + request.correo()));

        if (!usuario.isHabilitado()) {
            throw new CuentaNoHabilitadaException(usuario.getCorreo());
        }

        try {
            authenticationManager
                    .authenticate(new UsernamePasswordAuthenticationToken(request.correo(), request.password()));
        } catch (BadCredentialsException e) {
            publicadorAuditoria.publicarAcceso(usuario.getId(), ipCliente, EstadoEvento.FALLO,
                    "LOGIN_FALLIDO_CREDENCIALES");
            throw new CredencialesInvalidasException("Credenciales inválidas");
        }

        publicadorAuditoria.publicarAcceso(usuario.getId(), ipCliente, EstadoEvento.EXITO, "LOGIN_EXITOSO");

        return generarRespuestaAuth(usuario);
    }

    @Override
    @Transactional
    public RespuestaAutenticacion refrescarToken(SolicitudRefreshToken solicitud, String ipCliente) {
        String refreshToken = solicitud.refreshToken();
        String correo = jwtService.extraerSubject(refreshToken);

        Usuario usuario = usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new ExcepcionNoAutorizado("USUARIO_NO_ENCONTRADO"));

        // Validar rotación de tokens (Blacklist de refresh tokens usados)
        String key = "REFRESH_TOKEN:" + refreshToken;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(key))) {
            log.error("[AUTH-REF] Intento de reutilizar Refresh Token invalidado por rotación!");
            publicadorAuditoria.publicarAcceso(usuario.getId(), ipCliente, EstadoEvento.FALLO, "REUSO_TOKEN_DETECTADO");
            throw new ExcepcionNoAutorizado("TOKEN_REUTILIZADO");
        }

        // Invalidar el token anterior (Rotación)
        redisTemplate.opsForValue().set(key, "USADO", jwtService.obtenerExpiracionRefreshMs(), TimeUnit.MILLISECONDS);

        publicadorAuditoria.publicarAcceso(usuario.getId(), ipCliente, EstadoEvento.EXITO, "TOKEN_REFRESCADO");
        return generarRespuestaAuth(usuario);
    }

    @SuppressWarnings("null")
    @Override
    @Transactional
    public void registrarLogout(UUID usuarioId, String token, String ipCliente) {
        if (token != null) {
            // Añadir a Blacklist en Redis
            String key = "BLACKLIST_JWT:" + token;
            redisTemplate.opsForValue().set(key, usuarioId.toString(), 24, TimeUnit.HOURS);
        }
        publicadorAuditoria.publicarAcceso(usuarioId, ipCliente, EstadoEvento.EXITO, "LOGOUT_EXITOSO");
    }

    @SuppressWarnings("null")
    @Override
    @Transactional
    public void cambiarPassword(UUID usuarioId, SolicitudCambioPassword solicitud, String ipCliente) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        if (!passwordEncoder.matches(solicitud.passwordActual(), usuario.getPassword())) {
            publicadorAuditoria.publicarAcceso(usuarioId, ipCliente, EstadoEvento.FALLO, "CAMBIO_PASS_ERROR_ACTUAL");
            throw new IllegalArgumentException("La contraseña actual es incorrecta");
        }

        if (!solicitud.contrasenasNuevasCoinciden()) {
            throw new ContrasenasNoCoincidenException();
        }

        usuario.setPassword(passwordEncoder.encode(solicitud.nuevoPassword()));
        usuarioRepository.save(usuario);

        publicadorAuditoria.publicarAcceso(usuario.getId(), ipCliente, EstadoEvento.EXITO, "CAMBIO_PASSWORD_MANUAL");
        publicadorAuditoria.publicarNotificacionSeguridad(usuario.getId(), usuario.getCorreo(),
                "CAMBIO_PASSWORD_MANUAL");
    }

    @Override
    @Transactional
    public UUID registrar(SolicitudRegistro request, String ipCliente) {
        validarDisponibilidad(request);

        Rol rolBase = rolRepository.findByNombre(Rol.NombreRol.ROLE_FREE.name())
                .orElseThrow(() -> new IllegalStateException("Rol base no encontrado"));

        Usuario usuario = Usuario.builder()
                .nombreUsuario(request.nombreUsuario())
                .correo(request.correo())
                .password(passwordEncoder.encode(request.password()))
                .habilitado(false)
                .cuentaNoBloqueada(true)
                .build();
        usuario.getRoles().add(rolBase);

        Usuario guardado = usuarioRepository.save(usuario);
        publicadorAuditoria.publicarAcceso(guardado.getId(), ipCliente, EstadoEvento.EXITO, "REGISTRO_INICIAL");

        // Disparamos la generación del código de activación vía RabbitMQ
        publicadorAuditoria.publicarSolicitudOtp(FabricaSolicitudOtp.paraActivacionRegistroInicial(guardado));

        return guardado.getId();
    }

    @Override
    @Transactional
    public UUID iniciarRecuperacion(SolicitudRecuperacion solicitud) {
        Optional<Usuario> usuarioOpt = usuarioRepository.findByCorreoAndHabilitadoTrue(solicitud.correo());

        if (usuarioOpt.isEmpty()) {
            publicadorAuditoria.publicarAcceso(null, "SISTEMA-RECOVERY", EstadoEvento.FALLO,
                    "RECUPERACION_FALLIDA_USUARIO_NO_EXISTE");
            return UUID.randomUUID();
        }

        Usuario usuario = usuarioOpt.get();
        String telefonoAEnviar = (solicitud.tipo() == TipoVerificacion.SMS
                || solicitud.tipo() == TipoVerificacion.WHATSAPP)
                        ? servicioPerfilCache.obtenerTelefono(usuario.getId())
                        : null;

        publicadorAuditoria
                .publicarSolicitudOtp(
                        FabricaSolicitudOtp.paraRecuperacionPassword(usuario, solicitud.tipo(), telefonoAEnviar));

        return UUID.randomUUID();
    }

    @Override
    @Transactional
    public void restablecerPassword(UUID registroId, String codigoOtp, SolicitudRestablecerPassword solicitud) {
        UUID userId = clienteMensajeria.validarCodigoYObtenerUsuario(new SolicitudValidarRecuperacion(registroId, codigoOtp));
        if (userId == null)
            throw new TokenInvalidoException("Código inválido");

        Usuario usuario = usuarioRepository.findById(userId).orElseThrow();
        usuario.setPassword(passwordEncoder.encode(solicitud.nuevoPassword()));
        usuarioRepository.save(usuario);

        publicadorAuditoria.publicarAcceso(usuario.getId(), "SISTEMA-RECOVERY", EstadoEvento.EXITO,
                "RESETEO_PASSWORD_EXITOSO");
        publicadorAuditoria.publicarNotificacionSeguridad(usuario.getId(), usuario.getCorreo(), "RESETEO_PASSWORD");
    }

    private RespuestaAutenticacion generarRespuestaAuth(Usuario usuario) {
        java.util.Map<String, Object> claims = new HashMap<>();
        claims.put("usuarioId", usuario.getId().toString());

        String access = jwtService.generarToken(usuario, claims);
        String refresh = jwtService.generarRefreshToken(usuario);

        return RespuestaAutenticacion.of(
                access, refresh,
                jwtService.obtenerExpiracionMs(),
                jwtService.obtenerExpiracionRefreshMs(),
                usuario.getId().toString(),
                usuario.getNombreUsuario(),
                usuario.getRoles().stream().map(Rol::getNombre).toList());
    }

    private void validarDisponibilidad(SolicitudRegistro request) {
        if (usuarioRepository.existsByNombreUsuario(request.nombreUsuario()))
            throw new UsuarioYaExisteException("nombreUsuario", request.nombreUsuario());
        if (usuarioRepository.existsByCorreo(request.correo()))
            throw new UsuarioYaExisteException("correo", request.correo());
    }

    @SuppressWarnings("null")
    @Override
    public void eliminarCuenta(UUID usuarioId, String ipCliente) {
        Usuario usuario = usuarioRepository.findById(usuarioId).orElseThrow();
        usuario.setHabilitado(false);
        usuarioRepository.save(usuario);
        publicadorAuditoria.publicarAcceso(usuarioId, ipCliente, EstadoEvento.EXITO, "CUENTA_ELIMINADA");
    }

    @SuppressWarnings("null")
    @Override
    @Transactional
    public void solicitarOtpActivacion(com.usuario.aplicacion.dtos.solicitudes.SolicitudReenvioOtp solicitud) {
        Usuario usuario = usuarioRepository.findByCorreo(solicitud.email())
                .orElseThrow(() -> new UsuarioNoEncontradoException("Usuario no encontrado con correo: " + solicitud.email()));
        
        if (usuario.isHabilitado()) {
            throw new IllegalArgumentException("La cuenta ya se encuentra activada.");
        }

        publicadorAuditoria.publicarSolicitudOtp(
                FabricaSolicitudOtp.paraReenvioActivacionManual(usuario, solicitud.tipo(), solicitud.telefono()));
    }

    @Override
    @Transactional
    public void sincronizarTelefono(UUID usuarioId, String telefono) {
        log.info("[AUTH-SYNC] Sincronizando teléfono verificado para usuario: {}", usuarioId);

        // 1. Sincronizar con MS-CLIENTE
        if (telefono != null && !telefono.isBlank()) {
            try {
                clientePerfilExterno.actualizarTelefono(usuarioId, telefono);
                log.info("[AUTH-SYNC] Teléfono sincronizado exitosamente con MS-CLIENTE.");
            } catch (Exception e) {
                log.error("[AUTH-SYNC] Error sincronizando teléfono con MS-CLIENTE: {}", e.getMessage());
            }
        }

        // 2. Registrar evento de auditoría interna si fuera necesario (opcional)
        publicadorAuditoria.publicarAcceso(usuarioId, "SISTEMA-SYNC", EstadoEvento.EXITO, "TELEFONO_SINCRONIZADO_OTP");
    }
}
