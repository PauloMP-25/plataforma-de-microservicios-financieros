package com.usuario.aplicacion.servicios;

import com.usuario.aplicacion.dtos.NuevoPasswordDTO;
import com.usuario.aplicacion.dtos.RegistroAuditoriaDTO;
import com.usuario.infraestructura.clientes.ClienteAuditoria;
import com.usuario.aplicacion.dtos.RespuestaAutenticacion;
import com.usuario.aplicacion.dtos.RespuestaRegistro;
import com.usuario.aplicacion.dtos.SolicitudGenerarOtp;
import com.usuario.aplicacion.dtos.SolicitudLogin;
import com.usuario.aplicacion.dtos.SolicitudRecuperacion;
import com.usuario.aplicacion.dtos.SolicitudRegistro;
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

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class ServicioAutenticacion {

    private final jakarta.servlet.http.HttpServletRequest requestHttp;
    private final AuthenticationManager authenticationManager;
    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final ServicioJwt jwtService;
    private final ServicioBloqueoIp servicioBloqueoIp;
    private final PasswordEncoder passwordEncoder;
    private final ClienteAuditoria clienteAuditoria;
    private final ClientePerfilExterno clientePerfilExterno;
    private final ClienteMensajeria clienteMensajeria;
    private final PublicadorAuditoria publicadorAuditoria;
    private static final String MODULO = "MICROSERVICIO-USUARIO";

    // =========================================================================
    // Login (Optimizado: Menos código, misma seguridad)
    // =========================================================================
    @Transactional
    public RespuestaAutenticacion login(SolicitudLogin request, String ipCliente) {
        // Buscamos al usuario antes de autenticar para capturar su ID para la auditoría
        Usuario usuario = usuarioRepository.findByCorreo(request.correo())
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + request.correo()));

        if (usuario != null) {
            requestHttp.setAttribute("intento_usuario_id", usuario.getId());
        }

        // La autenticación lanzará excepciones que atrapará el ManejadorGlobal
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.correo(), request.password())
        );

        servicioBloqueoIp.loginExitoso(ipCliente);

        registrarEventoAuditoria(usuario.getNombreUsuario(), "LOGIN_EXITOSO", "Autenticación Completa", ipCliente);
        publicadorAuditoria.publicarAcceso(usuario.getId(), ipCliente, "EXITO", "LOGIN CORRECTO", PublicadorAuditoria.RK_ACCESO_LOGIN);

        log.info("Login exitoso — usuario: '{}'", usuario.getNombreUsuario());

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
    public RespuestaRegistro registrar(SolicitudRegistro request) {
        validarDisponibilidad(request);

        Rol rolPorDefecto = rolRepository.findByNombre(Rol.NombreRol.ROLE_FREE.name())
                .orElseThrow(() -> new IllegalStateException("Rol base no configurado."));

        Usuario usuario = Usuario.builder()
                .nombreUsuario(request.nombreUsuario())
                .correo(request.correo())
                .password(passwordEncoder.encode(request.password()))
                .habilitado(false)
                .cuentaNoBloqueada(true)
                .build();
        usuario.getRoles().add(rolPorDefecto);

        Usuario usuarioGuardado = usuarioRepository.save(usuario);

        // Orquestación con otros microservicios
        clientePerfilExterno.crearPerfilInicial(usuarioGuardado.getId());
        dispararOtp(usuarioGuardado, "ACTIVACION_CUENTA");

        registrarEventoAuditoria(usuarioGuardado.getNombreUsuario(), "REGISTRO_USUARIO", "Pendiente de validación", usuarioGuardado.getCorreo());
        return RespuestaRegistro.builder()
                .idUsuario(usuarioGuardado.getId().toString())
                .nombreUsuario(usuarioGuardado.getNombreUsuario())
                .correo(usuarioGuardado.getCorreo())
                .build();
    }

    // =========================================================================
    // Métodos de Soporte y Recuperación
    // =========================================================================
    @Transactional
    public void activarCuenta(UUID usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("ID no encontrado"));

        if (!usuario.isHabilitado()) {
            usuario.setHabilitado(true);
            usuarioRepository.save(usuario);
            registrarEventoAuditoria(usuario.getNombreUsuario(), "CUENTA_ACTIVADA", "OTP validado", usuario.getCorreo());
        }
    }

    // =========================================================================
    // Recuperación de Contraseña
    // =========================================================================
    @Transactional
    public void iniciarRecuperacion(SolicitudRecuperacion solicitud) {
        Usuario usuario = usuarioRepository.findByCorreo(solicitud.correo())
                .orElseThrow(() -> new IllegalArgumentException("Correo no encontrado"));

        // Disparamos a mensajería con un "tipo" diferente
        dispararOtp(usuario, "RESTABLECER_PASSWORD");
        registrarEventoAuditoria(usuario.getNombreUsuario(), "CAMBIAR_PASSWORD", "Pendiente de confirmacion", usuario.getCorreo());

        log.info("Proceso de recuperación iniciado para: {}", usuario.getCorreo());
    }

    @Transactional
    public void completarRecuperacion(NuevoPasswordDTO datos) {
        if (!datos.contrasenasCoinciden()) {
            throw new IllegalArgumentException("Contraseñas no coinciden");
        }

        UUID usuarioId = clienteMensajeria.validarCodigoYObtenerUsuario(datos.codigoOtp());
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalStateException("Usuario no encontrado tras validar OTP"));

        usuario.setPassword(passwordEncoder.encode(datos.nuevoPassword()));
        usuarioRepository.save(usuario);
        log.info("Password reseteado para: {}", usuario.getNombreUsuario());
    }

    // =========================================================================
    // Helpers Privados
    // =========================================================================
    private void validarDisponibilidad(SolicitudRegistro request) {
        if (!request.contrasenasCoinciden()) {
            throw new IllegalArgumentException("Contraseñas no coinciden");
        }
        if (usuarioRepository.existsByNombreUsuario(request.nombreUsuario())) {
            throw new IllegalStateException("Usuario duplicado");
        }
        if (usuarioRepository.existsByCorreo(request.correo())) {
            throw new IllegalStateException("Correo duplicado");
        }
    }

    private void dispararOtp(Usuario usuario, String tipo) {
        try {
            String propositoReal = tipo.equals("CAMBIAR_PASSWORD") ? "RESTABLECER_PASSWORD" : "ACTIVACION_CUENTA";
            clienteMensajeria.generarCodigo(new SolicitudGenerarOtp(usuario.getId(), usuario.getCorreo(), "EMAIL", propositoReal));
        } catch (Exception e) {
            log.error("Fallo al contactar MS-Mensajería para {}: {}", usuario.getCorreo(), e.getMessage());
        }
    }

    private void registrarEventoAuditoria(String usuario, String accion, String desc, String origen) {
        clienteAuditoria.enviar(new RegistroAuditoriaDTO(LocalDateTime.now(), usuario, accion, desc, origen, MODULO));
    }
}
