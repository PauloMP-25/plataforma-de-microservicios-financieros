package com.usuario.aplicacion.servicios;

import com.usuario.aplicacion.dtos.RespuestaAutenticacion;
import com.usuario.aplicacion.dtos.SolicitudLogin;
import com.usuario.aplicacion.dtos.SolicitudRegistro;
import com.usuario.dominio.entidades.TokenConfirmacionEmail;
import com.usuario.dominio.entidades.Rol;
import com.usuario.dominio.entidades.Usuario;
import com.usuario.dominio.repositorios.TokenConfirmacionEmailRepository;
import com.usuario.dominio.repositorios.RolRepository;
import com.usuario.dominio.repositorios.UsuarioRepository;
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
    private final TokenConfirmacionEmailRepository tokenRepository;
    private final ServicioJwt jwtService;
    private final ServicioBloqueoIp servicioBloqueoIp;
    private final PasswordEncoder passwordEncoder;
    
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

            log.info("Login exitoso — usuario: '{}', ip: {}", usuario.getNombreUsuario(), ipCliente);

            return RespuestaAutenticacion.of(
                    token,
                    jwtService.obtenerExpiracionJwt(),
                    usuario.getNombreUsuario(),
                    roles
            );

        } catch (BadCredentialsException ex) {
            boolean bloqueado = servicioBloqueoIp.loginFallido(ipCliente);

            if (bloqueado) {
                log.warn("IP bloqueada — ip: {}, usuario: {}", ipCliente, request.getNombreUsuario());
            } else {
                log.warn("Credenciales inválidas — ip: {}, usuario: {}", ipCliente, request.getNombreUsuario());
            }

            throw new BadCredentialsException("Credenciales inválidas.");

        } catch (DisabledException ex) {
            throw new DisabledException("La cuenta no ha sido activada. Revise su correo.");

        } catch (LockedException ex) {
            throw new LockedException("Cuenta bloqueada. Contacte con soporte.");
        }
    }

    // =========================================================================
    // Registro
    // =========================================================================

    @Transactional
    public String registrar(SolicitudRegistro request) {

        if (!request.contrasenasCoinciden()) {
            throw new IllegalArgumentException("Las contraseñas no coinciden.");
        }

        if (usuarioRepository.existsByNombreUsuario(request.getNombreUsuario())) {
            throw new IllegalStateException("El nombre de usuario ya está en uso.");
        }

        if (usuarioRepository.existsByCorreo(request.getCorreo())) {
            throw new IllegalStateException("El correo ya está registrado.");
        }

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
        Usuario usuarioGuardado = usuarioRepository.save(usuario);

        String token = generarTokenConfirmacion(usuarioGuardado);

        log.info("Usuario registrado — {}", usuarioGuardado.getNombreUsuario());

        return "Registro exitoso. Revise su correo.";
    }

    // =========================================================================
    // Confirmación de email
    // =========================================================================

    @Transactional
    public String confirmarCorreo(String tokenValor) {

        TokenConfirmacionEmail token = tokenRepository.findByToken(tokenValor)
                .orElseThrow(() -> new IllegalArgumentException("Token inválido."));

        if (token.estaConfirmado()) {
            throw new IllegalStateException("Token ya utilizado.");
        }

        if (token.estaExpirado()) {
            throw new IllegalStateException("El token ha expirado.");
        }

        token.setConfirmadoEn(LocalDateTime.now());
        tokenRepository.save(token);

        Usuario usuario = token.getUsuario();
        usuario.setHabilitado(true);
        usuarioRepository.save(usuario);

        log.info("Cuenta activada — {}", usuario.getNombreUsuario());

        return "Cuenta activada correctamente.";
    }

    // =========================================================================
    // Métodos privados
    // =========================================================================

    private String generarTokenConfirmacion(Usuario usuario) {

        String valorToken = UUID.randomUUID().toString();

        TokenConfirmacionEmail token = TokenConfirmacionEmail.builder()
                .token(valorToken)
                .usuario(usuario)
                .expiraEn(LocalDateTime.now().plusHours(24))
                .build();

        tokenRepository.save(token);

        return valorToken;
    }
}