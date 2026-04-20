package com.usuario.aplicacion.servicios;

import com.usuario.aplicacion.dtos.RegistroAuditoriaDTO;
import com.usuario.infraestructura.clientes.ClienteAuditoria;
import com.usuario.aplicacion.dtos.RespuestaAutenticacion;
import com.usuario.aplicacion.dtos.RespuestaRegistro;
import com.usuario.aplicacion.dtos.SolicitudGenerarOtp;
import com.usuario.aplicacion.dtos.SolicitudLogin;
import com.usuario.aplicacion.dtos.SolicitudRegistro;
import com.usuario.dominio.entidades.Rol;
import com.usuario.dominio.entidades.Usuario;
import com.usuario.dominio.repositorios.RolRepository;
import com.usuario.dominio.repositorios.UsuarioRepository;
import com.usuario.infraestructura.clientes.ClienteMensajeria;
import com.usuario.infraestructura.clientes.ClientePerfilExterno;
import com.usuario.infraestructura.seguridad.ServicioJwt;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.*;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ServicioAutenticacion {

    private final AuthenticationManager authenticationManager;
    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final ServicioJwt jwtService;
    private final ServicioBloqueoIp servicioBloqueoIp;
    private final PasswordEncoder passwordEncoder;
    private final ClienteAuditoria clienteAuditoria;
    private final ClientePerfilExterno clientePerfilExterno;
    private final ClienteMensajeria clienteMensajeria;
    private static final String MODULO = "MICROSERVICIO-USUARIO";

    // =========================================================================
    // Login
    // =========================================================================
    @Transactional
    public RespuestaAutenticacion login(SolicitudLogin request, String ipCliente) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getNombreUsuario(),
                            request.getPassword()
                    )
            );

            servicioBloqueoIp.loginExitoso(ipCliente);

            Usuario usuario = usuarioRepository.findByNombreUsuarioConRoles(request.getNombreUsuario())
                    .orElseThrow(() -> new UsernameNotFoundException(
                    "Usuario no encontrado tras autenticación: " + request.getNombreUsuario()));

            String token = jwtService.generarToken(usuario);

            List<String> roles = usuario.getRoles().stream()
                    .map(Rol::getNombre)
                    .collect(Collectors.toList());

            clienteAuditoria.enviar(new RegistroAuditoriaDTO(
                    LocalDateTime.now(),
                    usuario.getNombreUsuario(),
                    "LOGIN_EXITOSO",
                    "Autenticación Completa",
                    ipCliente,
                    MODULO
            ));

            log.info("Login exitoso — usuario: '{}', ip: {}", usuario.getNombreUsuario(), ipCliente);

            return RespuestaAutenticacion.of(
                    token,
                    jwtService.obtenerExpiracionJwt(),
                    usuario.getId().toString(),
                    usuario.getNombreUsuario(),
                    roles
            );

        } catch (BadCredentialsException ex) {
            // 1. Registramos el fallo y verificamos si se debe bloquear
            boolean ahoraBloqueado = servicioBloqueoIp.loginFallido(ipCliente);

            // 2. Solo enviamos auditoría de LOGIN_FALLIDO si la IP NO se bloqueó en este paso
            // (para evitar duplicar con la auditoría de IP_BLOQUEADA que ya hace el servicio)
            if (!ahoraBloqueado) {
                clienteAuditoria.enviar(new RegistroAuditoriaDTO(
                        LocalDateTime.now(),
                        request.getNombreUsuario(),
                        "LOGIN_FALLIDO",
                        "Credenciales inválidas",
                        ipCliente,
                        MODULO));
            }
            log.warn("Intento fallido — ip: {}, usuario: {}, ¿Bloqueado?: {}",
                    ipCliente, request.getNombreUsuario(), ahoraBloqueado);
            throw new BadCredentialsException("Credenciales inválidas.");
        } catch (DisabledException ex) {
            clienteAuditoria.enviar(new RegistroAuditoriaDTO(
                    LocalDateTime.now(),
                    request.getNombreUsuario(),
                    "LOGIN_FALLIDO",
                    "El correo aun no ha sido confirmado",
                    ipCliente,
                    MODULO
            ));
            throw new DisabledException("La cuenta no ha sido activada. Revise su correo.");

        } catch (LockedException ex) {
            clienteAuditoria.enviar(new RegistroAuditoriaDTO(
                    LocalDateTime.now(),
                    request.getNombreUsuario(),
                    "LOGIN_FALLIDO",
                    "Cuenta bloqueada por intentos fallidos",
                    ipCliente,
                    MODULO
            ));
            throw new LockedException("Cuenta bloqueada. Contacte con soporte.");
        }
    }

    // =========================================================================
    // Registro
    // =========================================================================
    @Transactional
    public RespuestaRegistro registrar(SolicitudRegistro request) {
        if (!request.contrasenasCoinciden()) {
            throw new IllegalArgumentException("Las contraseñas no coinciden.");
        }

        if (usuarioRepository.existsByNombreUsuario(request.getNombreUsuario())) {
            throw new IllegalStateException("El nombre de usuario ya está en uso.");
        }

        if (usuarioRepository.existsByCorreo(request.getCorreo())) {
            throw new IllegalStateException("El correo ya está registrado.");
        }
        // 2. Obtención de Rol y Construcción de Entidad
        Rol rolPorDefecto = rolRepository.findByNombre(Rol.NombreRol.ROLE_FREE.name())
                .orElseThrow(() -> new IllegalStateException("Rol ROLE_FREE no encontrado."));

        Usuario usuario = Usuario.builder()
                .nombreUsuario(request.getNombreUsuario())
                .correo(request.getCorreo())
                .password(passwordEncoder.encode(request.getPassword()))
                .habilitado(false)
                .cuentaNoBloqueada(true)
                .build();

        usuario.getRoles().add(rolPorDefecto);
        
        // 3. Persistencia
        Usuario usuarioGuardado = usuarioRepository.save(usuario);

        // 4. Creación de Perfil (Invocación Síncrona a MS-Perfiles)
        clientePerfilExterno.crearPerfilInicial(usuarioGuardado.getId());
        
        // 5. DISPARADOR DE MENSAJERÍA (Con manejo de resiliencia básico)
        try {
            SolicitudGenerarOtp solicitudOtp = new SolicitudGenerarOtp(
                    usuarioGuardado.getId(),
                    usuarioGuardado.getCorreo(),
                    "EMAIL"
            );
            clienteMensajeria.generarCodigo(solicitudOtp);
            log.info("Evento de OTP enviado a mensajería para usuario: {}", usuarioGuardado.getId());
        } catch (Exception e) {
            log.error("No se pudo disparar el OTP para {}: {}", usuarioGuardado.getCorreo(), e.getMessage());
            // Nota: Aquí podrías decidir si lanzar una excepción o dejar que el usuario 
            // solicite un reenvío después desde el frontend.
        }

        // 6. Auditoría y Respuesta
        clienteAuditoria.enviar(new RegistroAuditoriaDTO(
                LocalDateTime.now(),
                usuarioGuardado.getNombreUsuario(),
                "REGISTRO_USUARIO",
                "Nuevo usuario registrado - Pendiente de validación",
                usuarioGuardado.getCorreo(),
                MODULO
        ));

        return RespuestaRegistro.builder()
                .idUsuario(usuario.getId().toString())
                .nombreUsuario(usuarioGuardado.getNombreUsuario())
                .correo(usuarioGuardado.getCorreo())
                .build();
    }

    // =========================================================================
    // Activación de Cuenta (Invocado por MS-Mensajería)
    // =========================================================================
    @Transactional
    public void activarCuenta(UUID usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado con ID: " + usuarioId));

        if (usuario.isHabilitado()) {
            log.warn("Intento de activación en cuenta ya habilitada: {}", usuarioId);
            return;
        }

        usuario.setHabilitado(true);
        usuarioRepository.save(usuario);

        // Enviamos auditoría de la activación
        clienteAuditoria.enviar(new RegistroAuditoriaDTO(
                LocalDateTime.now(),
                usuario.getNombreUsuario(),
                "CUENTA_ACTIVADA",
                "Cuenta confirmada mediante código OTP",
                usuario.getCorreo(),
                MODULO
        ));

        log.info("Cuenta activada exitosamente para el usuario: {}", usuario.getNombreUsuario());
    }
}
