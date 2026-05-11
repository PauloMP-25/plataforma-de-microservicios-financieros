package com.usuario.aplicacion.servicios.implementacion;

import com.libreria.comun.enums.EstadoEvento;
import com.libreria.comun.seguridad.ServicioJwt;
import com.usuario.aplicacion.dtos.*;
import com.usuario.aplicacion.excepciones.CuentaNoHabilitadaException;
import com.usuario.aplicacion.excepciones.TokenInvalidoException;
import com.usuario.aplicacion.servicios.IServicioAutenticacion;
import com.usuario.dominio.entidades.Rol;
import com.usuario.dominio.entidades.Usuario;
import com.usuario.dominio.repositorios.RolRepository;
import com.usuario.dominio.repositorios.UsuarioRepository;
import com.usuario.infraestructura.clientes.ClienteMensajeria;
import com.usuario.infraestructura.clientes.ClientePerfilExterno;
import com.usuario.infraestructura.mensajeria.PublicadorAuditoria;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

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
    private final ClienteMensajeria clienteMensajeria;
    private final PublicadorAuditoria publicadorAuditoria;

    @Override
    @Transactional
    public void activarCuenta(UUID usuarioId, String telefono, String ipCliente) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("ID de usuario no encontrado: " + usuarioId));

        if (!usuario.isHabilitado()) {
            if (telefono != null && !telefono.isBlank()) {
                try {
                    clientePerfilExterno.actualizarTelefono(usuarioId, telefono);
                    log.info("[MS-USUARIO] Teléfono verificado y guardado para usuario: {}", usuarioId);
                } catch (Exception e) {
                    log.error("No se pudo persistir el teléfono en MS-CLIENTE: {}", e.getMessage());
                }
            }

            usuario.setHabilitado(true);
            usuario.setCuentaNoBloqueada(true);
            usuarioRepository.save(usuario);

            publicadorAuditoria.publicarAcceso(usuario.getId(), ipCliente,
                    EstadoEvento.EXITO, "CUENTA_ACTIVADA_VIA_OTP");
        }
    }

    @Override
    @Transactional
    public void cambiarPassword(UUID usuarioId, SolicitudCambioPassword solicitud, String ipCliente) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        validarPasswordActual(usuario, solicitud.passwordActual(), ipCliente);

        if (!solicitud.contrasenasNuevasCoinciden()) {
            throw new IllegalArgumentException("Las nuevas contraseñas no coinciden");
        }

        usuario.setPassword(passwordEncoder.encode(solicitud.nuevoPassword()));
        usuarioRepository.save(usuario);
        
        publicadorAuditoria.publicarAcceso(usuario.getId(), ipCliente, EstadoEvento.EXITO, "CAMBIO_PASSWORD_PERFIL");
    }

    @Override
    @Transactional
    public UUID iniciarRecuperacion(SolicitudRecuperacion solicitud) {
        Usuario usuario = usuarioRepository.findByCorreoAndHabilitadoTrue(solicitud.correo())
                .orElseThrow(() -> new IllegalArgumentException("El correo ingresado no está registrado o activo."));

        String telefonoAEnviar = solicitud.telefono();

        if (solicitud.tipo() == TipoVerificacion.SMS) {
            String telefonoRegistrado = clientePerfilExterno.obtenerTelefono(usuario.getId());
            if (telefonoRegistrado == null || !telefonoRegistrado.equals(solicitud.telefono())) {
                throw new IllegalArgumentException("El teléfono no coincide con nuestros registros.");
            }
            telefonoAEnviar = telefonoRegistrado;
        }

        clienteMensajeria.validarLimite(new SolicitudGenerarOtp(
                usuario.getId(),
                usuario.getCorreo(),
                telefonoAEnviar,
                solicitud.tipo(),
                PropositoCodigo.RESTABLECER_PASSWORD));
        
        UUID registroId = clienteMensajeria.generarCodigo(new SolicitudGenerarOtp(
            usuario.getId(),
            usuario.getCorreo(),
            telefonoAEnviar,
            solicitud.tipo(),
            PropositoCodigo.RESTABLECER_PASSWORD));

        publicadorAuditoria.publicarSolicitudOtp(new SolicitudGenerarOtp(
                usuario.getId(),
                usuario.getCorreo(),
                telefonoAEnviar,
                solicitud.tipo(),
                PropositoCodigo.RESTABLECER_PASSWORD));
                
        return registroId;
    }

    @Override
    @Transactional
    public void restablecerPassword(UUID registroId, String codigoOtp, SolicitudRestablecerPassword solicitud) {
        if (!solicitud.contrasenasNuevasCoinciden()) {
            throw new IllegalArgumentException("Las contraseñas no coinciden");
        }
        
        UUID idUsuarioConfirmado = clienteMensajeria.validarCodigoYObtenerUsuario(registroId, codigoOtp);

        if (idUsuarioConfirmado == null) {
            throw new TokenInvalidoException("El código de verificación es inválido o ha expirado.");
        }

        Usuario usuario = usuarioRepository.findById(idUsuarioConfirmado)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado tras validación OTP"));

        usuario.setPassword(passwordEncoder.encode(solicitud.nuevoPassword()));
        usuarioRepository.save(usuario);
        
        publicadorAuditoria.publicarAcceso(usuario.getId(), "SISTEMA", EstadoEvento.EXITO, "RESETEO_PASSWORD_OTP");
    }

    @Override
    @Transactional
    public void eliminarCuenta(UUID usuarioId, String ipCliente) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("ID de usuario no encontrado"));

        usuario.setHabilitado(false);
        usuario.setCuentaNoBloqueada(false);
        usuarioRepository.save(usuario);

        publicadorAuditoria.publicarAcceso(usuario.getId(), ipCliente, EstadoEvento.EXITO, "ELIMINACION_LOGICA_CUENTA");
    }

    @Override
    @Transactional
    public RespuestaAutenticacion login(SolicitudLogin request, String ipCliente) {
        Usuario usuario = usuarioRepository.findByCorreo(request.correo())
                .orElseThrow(() -> new UsernameNotFoundException("Credenciales inválidas"));

        if (!usuario.isHabilitado()) {
            throw new CuentaNoHabilitadaException(usuario.getCorreo());
        }

        if (!usuario.isAccountNonLocked()) {
            throw new LockedException("Su cuenta ha sido bloqueada temporalmente por seguridad.");
        }

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.correo(), request.password()));
        } catch (BadCredentialsException e) {
            publicadorAuditoria.publicarAcceso(usuario.getId(), ipCliente, EstadoEvento.FALLO, "LOGIN_FALLIDO_CREDENCIALES");
            throw e;
        }

        publicadorAuditoria.publicarAcceso(usuario.getId(), ipCliente, EstadoEvento.EXITO, "LOGIN_EXITOSO");

        // Preparamos los claims adicionales para el token
        java.util.Map<String, Object> claims = new java.util.HashMap<>();
        claims.put("usuarioId", usuario.getId().toString());

        return RespuestaAutenticacion.of(
                jwtService.generarToken(usuario, claims),
                jwtService.obtenerExpiracionMs(),
                usuario.getId().toString(),
                usuario.getNombreUsuario(),
                usuario.getRoles().stream().map(Rol::getNombre).toList());
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

        try {
            clientePerfilExterno.crearPerfilInicial(guardado.getId());
        } catch (Exception e) {
            log.warn("Perfil no creado inmediatamente: {}", e.getMessage());
        }

        publicadorAuditoria.publicarAcceso(guardado.getId(), ipCliente, EstadoEvento.EXITO, "REGISTRO_INICIAL");

        return guardado.getId();
    }

    @Override
    @Transactional
    public void registrarLogout(UUID usuarioId, String ipCliente) {
        publicadorAuditoria.publicarAcceso(usuarioId, ipCliente, EstadoEvento.EXITO, "LOGOUT_EXITOSO");
    }

    @Override
    @Transactional
    public void solicitarOtpActivacion(UUID usuarioId, SolicitudGenerarOtp solicitud) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        if (solicitud.tipo() == TipoVerificacion.SMS
                && (solicitud.telefono() == null || solicitud.telefono().isBlank())) {
            throw new IllegalArgumentException("El teléfono es obligatorio para la verificación por SMS.");
        }

        clienteMensajeria.validarLimite(new SolicitudGenerarOtp(
                usuario.getId(),
                usuario.getCorreo(),
                solicitud.telefono(),
                solicitud.tipo(),
                PropositoCodigo.ACTIVACION_CUENTA));

        publicadorAuditoria.publicarSolicitudOtp(new SolicitudGenerarOtp(
                usuario.getId(),
                usuario.getCorreo(),
                solicitud.telefono(),
                solicitud.tipo(),
                PropositoCodigo.ACTIVACION_CUENTA));
    }

    private void validarDisponibilidad(SolicitudRegistro request) {
        if (!request.contrasenasCoinciden()) {
            throw new IllegalArgumentException("Las contraseñas ingresadas no coinciden");
        }
        if (usuarioRepository.existsByNombreUsuario(request.nombreUsuario())) {
            throw new IllegalStateException("El nombre de usuario ya se encuentra en uso");
        }
        if (usuarioRepository.existsByCorreo(request.correo())) {
            throw new IllegalStateException("El correo electrónico ya está registrado");
        }
    }

    private void validarPasswordActual(Usuario usuario, String passwordActual, String ipCliente) {
        if (!passwordEncoder.matches(passwordActual, usuario.getPassword())) {
            publicadorAuditoria.publicarAcceso(usuario.getId(), ipCliente, EstadoEvento.FALLO, "PASSWORD_ACTUAL_INCORRECTA");
            throw new IllegalArgumentException("La contraseña actual es incorrecta");
        }
    }
}
