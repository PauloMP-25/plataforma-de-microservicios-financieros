package com.usuario.aplicacion.servicios;

import com.usuario.aplicacion.dtos.EstadoAcceso;
import com.usuario.aplicacion.dtos.PropositoCodigo;
import com.usuario.aplicacion.dtos.RespuestaAutenticacion;
import com.usuario.aplicacion.dtos.SolicitudCambioPassword;
import com.usuario.aplicacion.dtos.SolicitudGenerarOtp;
import com.usuario.aplicacion.dtos.SolicitudLogin;
import com.usuario.aplicacion.dtos.SolicitudRecuperacion;
import com.usuario.aplicacion.dtos.SolicitudRegistro;
import com.usuario.aplicacion.dtos.TipoVerificacion;
import com.usuario.dominio.entidades.Rol;
import com.usuario.dominio.entidades.Usuario;
import com.usuario.dominio.repositorios.RolRepository;
import com.usuario.dominio.repositorios.UsuarioRepository;
import com.usuario.infraestructura.clientes.ClienteMensajeria;
import com.usuario.infraestructura.clientes.ClientePerfilExterno;
import com.usuario.infraestructura.mensajeria.PublicadorAuditoria;
import com.usuario.infraestructura.seguridad.ServicioJwt;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.*;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class ServicioAutenticacion {

    private final AuthenticationManager authenticationManager;
    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final ServicioJwt jwtService;
    private final PasswordEncoder passwordEncoder;
    private final ClientePerfilExterno clientePerfilExterno;
    private final ClienteMensajeria clienteMensajeria;
    private final PublicadorAuditoria publicadorAuditoria;

    // =========================================================================
    // Activar la cuenta del usuario
    // =========================================================================
    @Transactional
    public void activarCuenta(UUID usuarioId, String ipCliente) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("ID no encontrado"));

        if (!usuario.isHabilitado()) {
            usuario.setHabilitado(true);
            usuarioRepository.save(usuario);
            publicadorAuditoria.publicarAcceso(usuario.getId(),
                    ipCliente,
                    EstadoAcceso.EXITO, "CUENTA_ACTIVADA_VIA_OTP", PublicadorAuditoria.RK_ACCESO_LOGIN);
            log.info("[MS-USUARIO] Usuario {} habilitado con éxito", usuarioId);
        }
    }

    // =========================================================================
    // Cambiar de Contraseña desde Perfil
    // =========================================================================
    @Transactional
    public void cambiarPassword(UUID usuarioId, SolicitudCambioPassword solicitud, String ipCliente) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        validarPasswordActual(usuario, solicitud.passwordActual(), ipCliente);

        if (!solicitud.contrasenasNuevasCoinciden()) {
            throw new IllegalArgumentException("Las contraseñas no coinciden");
        }

        usuario.setPassword(passwordEncoder.encode(solicitud.nuevoPassword()));
        usuarioRepository.save(usuario);
        publicadorAuditoria.publicarAcceso(usuario.getId(), ipCliente, EstadoAcceso.EXITO, "CAMBIO_PASSWORD_PERFIL", PublicadorAuditoria.RK_ACCESO_LOGIN);
    }

    // =========================================================================
    // Recuperación de Contraseña
    // =========================================================================
    @Transactional
    public void iniciarRecuperacion(SolicitudRecuperacion solicitud) {
        Usuario usuario = usuarioRepository.findByCorreo(solicitud.correo())
                .orElseThrow(() -> new IllegalArgumentException("Correo no encontrado"));

        publicadorAuditoria.publicarSolicitudOtp(new SolicitudGenerarOtp(
                usuario.getId(),
                usuario.getCorreo(),
                null,
                TipoVerificacion.EMAIL,
                PropositoCodigo.RESTABLECER_PASSWORD
        ));

        publicadorAuditoria.publicarAcceso(usuario.getId(), "N/A", EstadoAcceso.EXITO, "INICIO_RECUPERACION_PASSWORD", PublicadorAuditoria.RK_ACCESO_LOGIN);
    }

    @Transactional
    public void restablecerPassword(String codigoOtp, SolicitudCambioPassword solicitud) {
        if (!solicitud.contrasenasNuevasCoinciden()) {
            throw new IllegalArgumentException("Las contraseñas no coinciden");
        }

        // El MS-Mensajería valida el código y nos devuelve el ID del usuario
        UUID usuarioId = clienteMensajeria.validarCodigoYObtenerUsuario(codigoOtp);
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalStateException("Usuario no encontrado tras validación OTP"));

        usuario.setPassword(passwordEncoder.encode(solicitud.nuevoPassword()));
        usuarioRepository.save(usuario);
        publicadorAuditoria.publicarAcceso(usuario.getId(), "N/A", EstadoAcceso.EXITO, "RESETEO_PASSWORD_OTP", PublicadorAuditoria.RK_ACCESO_LOGIN);
    }

    // =========================================================================
    // Eliminar cuenta
    // =========================================================================
    @Transactional
    public void eliminarCuenta(UUID usuarioId, String ipCliente) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("ID no encontrado"));

        usuario.setHabilitado(false);
        usuario.setCuentaNoBloqueada(false);
        usuarioRepository.save(usuario);

        publicadorAuditoria.publicarAcceso(usuario.getId(), ipCliente, EstadoAcceso.EXITO, "ELIMINACION_LOGICA_CUENTA", PublicadorAuditoria.RK_ACCESO_LOGIN);
    }

    // =========================================================================
    // Login (Optimizado: Menos código, misma seguridad)
    // =========================================================================
    @Transactional
    public RespuestaAutenticacion login(SolicitudLogin request, String ipCliente) {
        Usuario usuario = usuarioRepository.findByCorreo(request.correo())
                .orElseThrow(() -> new UsernameNotFoundException("Credenciales inválidas"));

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.correo(), request.password())
        );

        publicadorAuditoria.publicarAcceso(usuario.getId(), ipCliente, EstadoAcceso.EXITO, "LOGIN_EXITOSO", PublicadorAuditoria.RK_ACCESO_LOGIN);

        return RespuestaAutenticacion.of(
                jwtService.generarToken(usuario),
                jwtService.obtenerExpiracionJwt(),
                usuario.getId().toString(),
                usuario.getNombreUsuario(),
                usuario.getRoles().stream().map(Rol::getNombre).toList()
        );
    }

    // =========================================================================
    // Registro
    // =========================================================================
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

        // 1. Perfil: Si esto es crítico, mantenlo síncrono o usa un try-catch
        try {
            clientePerfilExterno.crearPerfilInicial(guardado.getId());
        } catch (Exception e) {
            log.warn("MS-Cliente no disponible, el perfil se creará luego.");
        }

        // 2. OTP: No importa si MS-Mensajería está caído, el mensaje se guarda en RabbitMQ
        publicadorAuditoria.publicarSolicitudOtp(new SolicitudGenerarOtp(
                guardado.getId(),
                guardado.getCorreo(),
                null,
                TipoVerificacion.EMAIL,
                PropositoCodigo.ACTIVACION_CUENTA
        ));

        publicadorAuditoria.publicarAcceso(guardado.getId(), ipCliente, EstadoAcceso.EXITO, "REGISTRO_INICIAL", PublicadorAuditoria.RK_ACCESO_LOGIN);

        return guardado.getId();
    }

    // =========================================================================
    // Cerrar Sesión
    // =========================================================================
    @Transactional
    public void registrarLogout(UUID usuarioId, String ipCliente) {
        publicadorAuditoria.publicarAcceso(usuarioId, ipCliente, EstadoAcceso.EXITO, "LOGOUT_EXITOSO", PublicadorAuditoria.RK_ACCESO_LOGIN);
    }

    // =========================================================================
    // Helpers Privados
    // =========================================================================
    private void validarDisponibilidad(SolicitudRegistro request) {
        if (!request.contrasenasCoinciden()) {
            throw new IllegalArgumentException("Contraseñas no coinciden");
        }
        if (usuarioRepository.existsByNombreUsuario(request.nombreUsuario())) {
            throw new IllegalStateException("Nombre de usuario ocupado");
        }
        if (usuarioRepository.existsByCorreo(request.correo())) {
            throw new IllegalStateException("Correo ya registrado");
        }
    }

    private void validarPasswordActual(Usuario usuario, String passwordActual, String ipCliente) {
        if (!passwordEncoder.matches(passwordActual, usuario.getPassword())) {
            publicadorAuditoria.publicarAcceso(usuario.getId(), ipCliente, EstadoAcceso.FALLO, "PASSWORD_ACTUAL_INCORRECTA", PublicadorAuditoria.RK_ACCESO_FALLO);
            throw new IllegalArgumentException("La contraseña actual es incorrecta");
        }
    }
}
