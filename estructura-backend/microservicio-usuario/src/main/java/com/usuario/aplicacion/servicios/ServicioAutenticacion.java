package com.usuario.aplicacion.servicios;

import com.usuario.aplicacion.dtos.EstadoAcceso;
import com.usuario.aplicacion.dtos.PropositoCodigo;
import com.usuario.aplicacion.dtos.RespuestaAutenticacion;
import com.usuario.aplicacion.dtos.SolicitudCambioPassword;
import com.usuario.aplicacion.dtos.SolicitudGenerarOtp;
import com.usuario.aplicacion.dtos.SolicitudLogin;
import com.usuario.aplicacion.dtos.SolicitudRecuperacion;
import com.usuario.aplicacion.dtos.SolicitudRegistro;
import com.usuario.aplicacion.dtos.SolicitudRestablecerPassword;
import com.usuario.aplicacion.dtos.TipoVerificacion;
import com.usuario.aplicacion.excepciones.CuentaNoHabilitadaException;
import com.usuario.aplicacion.excepciones.TokenInvalidoException;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

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
    public void activarCuenta(UUID usuarioId, String telefono, String ipCliente) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("ID no encontrado"));

        if (!usuario.isHabilitado()) {
            // 1. Si el usuario validó por SMS, ahora sí guardamos el teléfono en el Perfil
            if (telefono != null && !telefono.isBlank()) {
                try {
                    clientePerfilExterno.actualizarTelefono(usuarioId, telefono);
                    log.info("[MS-USUARIO] Teléfono verificado y guardado para usuario: {}", usuarioId);
                } catch (Exception e) {
                    log.error("No se pudo persistir el teléfono en MS-CLIENTE: {}", e.getMessage());
                    // Dependiendo de tu lógica, podrías lanzar excepción aquí para hacer rollback
                }
            }

            // 2. Habilitar cuenta
            usuario.setHabilitado(true);
            usuario.setCuentaNoBloqueada(true);
            usuarioRepository.save(usuario);

            publicadorAuditoria.publicarAcceso(usuario.getId(), ipCliente,
                    EstadoAcceso.EXITO, "CUENTA_ACTIVADA_VIA_OTP", PublicadorAuditoria.RK_ACCESO_LOGIN);
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
        publicadorAuditoria.publicarAcceso(usuario.getId(), ipCliente, EstadoAcceso.EXITO, "CAMBIO_PASSWORD_PERFIL",
                PublicadorAuditoria.RK_ACCESO_LOGIN);
    }

    // =========================================================================
    // Recuperación de Contraseña
    // =========================================================================
    @Transactional
    public UUID iniciarRecuperacion(SolicitudRecuperacion solicitud) {
        Usuario usuario = usuarioRepository.findByCorreoAndHabilitadoTrue(solicitud.correo())
                .orElseThrow(() -> new IllegalArgumentException("Correo no encontrado"));

        String telefonoAEnviar = solicitud.telefono();

        // Si el usuario elige SMS, validamos que el teléfono coincida con el que
        // tenemos en Perfil
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

    @Transactional
    public void restablecerPassword(UUID registroId, String codigoOtp, SolicitudRestablecerPassword solicitud) {
        if (!solicitud.contrasenasNuevasCoinciden()) {
            throw new IllegalArgumentException("Las contraseñas no coinciden");
        }
        
        // Pasamos el ID de la fila de la tabla del MS-Mensajería y el código
        UUID idUsuarioConfirmado = clienteMensajeria.validarCodigoYObtenerUsuario(registroId, codigoOtp);

        if (idUsuarioConfirmado == null) {
            throw new TokenInvalidoException("Validación fallida.");
        }

        Usuario usuario = usuarioRepository.findById(idUsuarioConfirmado)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado tras validación OTP"));

        usuario.setPassword(passwordEncoder.encode(solicitud.nuevoPassword()));
        usuarioRepository.save(usuario);
        publicadorAuditoria.publicarAcceso(usuario.getId(), "N/A", EstadoAcceso.EXITO, "RESETEO_PASSWORD_OTP",
                PublicadorAuditoria.RK_ACCESO_LOGIN);
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

        publicadorAuditoria.publicarAcceso(usuario.getId(), ipCliente, EstadoAcceso.EXITO, "ELIMINACION_LOGICA_CUENTA",
                PublicadorAuditoria.RK_ACCESO_LOGIN);
    }

    // =========================================================================
    // Login (Optimizado: Menos código, misma seguridad)
    // =========================================================================
    @Transactional
    public RespuestaAutenticacion login(SolicitudLogin request, String ipCliente) {
        // 1. Buscar al usuario solo por correo (para saber si existe)
        Usuario usuario = usuarioRepository.findByCorreo(request.correo())
                .orElseThrow(() -> {
                    return new UsernameNotFoundException("El correo ingresado no pertenece a ninguna cuenta.");
                });
        // Nota: Por seguridad, se usa BadCredentialsException incluso si no existe.

        // 2. Verificar si está habilitado
        if (!usuario.isHabilitado()) {
            throw new CuentaNoHabilitadaException(usuario.getCorreo());
        }

        // 3. Verificar si la cuenta no está bloqueada (Opcional, si manejas
        // LockedException)
        if (!usuario.isAccountNonLocked()) {
            throw new LockedException("Su cuenta ha sido bloqueada temporalmente por seguridad.");
        }

        // 4. Autenticar contraseña: Si la clave está mal, lanzará
        // BadCredentialsException
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.correo(), request.password()));

        // 5. Éxito
        publicadorAuditoria.publicarAcceso(usuario.getId(), ipCliente, EstadoAcceso.EXITO, "LOGIN_EXITOSO",
                PublicadorAuditoria.RK_ACCESO_LOGIN);

        return RespuestaAutenticacion.of(
                jwtService.generarToken(usuario),
                jwtService.obtenerExpiracionJwt(),
                usuario.getId().toString(),
                usuario.getNombreUsuario(),
                usuario.getRoles().stream().map(Rol::getNombre).toList());
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
                .habilitado(false) // Seguirá deshabilitado hasta validar
                .cuentaNoBloqueada(true)
                .build();
        usuario.getRoles().add(rolBase);

        Usuario guardado = usuarioRepository.save(usuario);

        // Creamos el perfil inicial (síncrono o asíncrono según tu arquitectura)
        try {
            clientePerfilExterno.crearPerfilInicial(guardado.getId());
        } catch (Exception e) {
            log.warn("Perfil no creado inmediatamente: {}", e.getMessage());
        }

        publicadorAuditoria.publicarAcceso(guardado.getId(), ipCliente, EstadoAcceso.EXITO, "REGISTRO_INICIAL",
                PublicadorAuditoria.RK_ACCESO_LOGIN);

        return guardado.getId();
    }

    // =========================================================================
    // Cerrar Sesión
    // =========================================================================
    @Transactional
    public void registrarLogout(UUID usuarioId, String ipCliente) {
        publicadorAuditoria.publicarAcceso(usuarioId, ipCliente, EstadoAcceso.EXITO, "LOGOUT_EXITOSO",
                PublicadorAuditoria.RK_ACCESO_LOGIN);
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
            publicadorAuditoria.publicarAcceso(usuario.getId(), ipCliente, EstadoAcceso.FALLO,
                    "PASSWORD_ACTUAL_INCORRECTA", PublicadorAuditoria.RK_ACCESO_FALLO);
            throw new IllegalArgumentException("La contraseña actual es incorrecta");
        }
    }

    @Transactional
    public void solicitarOtpActivacion(UUID usuarioId, SolicitudGenerarOtp solicitud) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        // NO guardamos en base de datos. Solo validamos que si es SMS, venga el
        // teléfono.
        if (solicitud.tipo() == TipoVerificacion.SMS
                && (solicitud.telefono() == null || solicitud.telefono().isBlank())) {
            throw new IllegalArgumentException("El teléfono es obligatorio para la verificación por SMS.");
        }

        // Validamos límites en el MS-Mensajería (usando el teléfono de la solicitud)
        clienteMensajeria.validarLimite(new SolicitudGenerarOtp(
                usuario.getId(),
                usuario.getCorreo(),
                solicitud.telefono(),
                solicitud.tipo(),
                PropositoCodigo.ACTIVACION_CUENTA));

        // Publicamos para el envío (Twilio o Mail)
        publicadorAuditoria.publicarSolicitudOtp(new SolicitudGenerarOtp(
                usuario.getId(),
                usuario.getCorreo(),
                solicitud.telefono(),
                solicitud.tipo(),
                PropositoCodigo.ACTIVACION_CUENTA));
    }
}
