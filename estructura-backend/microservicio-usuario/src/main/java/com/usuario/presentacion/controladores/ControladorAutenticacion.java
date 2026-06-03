package com.usuario.presentacion.controladores;

import com.libreria.comun.respuesta.ResultadoApi;
import com.usuario.aplicacion.dtos.solicitudes.*;
import com.usuario.aplicacion.dtos.respuestas.*;
import com.usuario.aplicacion.puertos.IServicioAutenticacion;
import com.usuario.dominio.entidades.Usuario;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador de autenticación y gestión de usuarios.
 * Sigue los estándares de la plataforma LUKA utilizando ResultadoApi para todas las respuestas.
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Autenticación (Auth)", description = "Controlador principal para el registro, activación, inicio de sesión, recuperación de credenciales y desactivación de cuentas.")
public class ControladorAutenticacion {

    private final IServicioAutenticacion servicioAuth;

    /**
     * Activa la cuenta de un usuario recién registrado validando un código OTP.
     */
    @PutMapping("/activar")
    @Operation(summary = "Activar Cuenta con OTP", description = "Activa el perfil de usuario recién registrado validando el código OTP enviado a su correo o teléfono. Si el OTP es correcto, cambia el estado del usuario a habilitado.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cuenta activada exitosamente. El usuario ya puede iniciar sesión."),
            @ApiResponse(responseCode = "400", description = "Código OTP inválido, expirado o ya utilizado."),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado en el sistema.")
    })
    public ResponseEntity<ResultadoApi<String>> activarCuenta(
            @RequestParam @Parameter(description = "Correo electrónico del usuario.", example = "correo@gmail.com") String correo,
            @RequestParam(required = false) @Parameter(description = "Código OTP de verificación de 6 dígitos.", example = "123456") String codigoOtp,
            @RequestParam(required = false) @Parameter(description = "Número telefónico opcional para registrar o validar.", example = "+51999999999") String telefono,
            HttpServletRequest request) {
        
        log.info("[API] Intento de activación para usuario con correo: {}", correo);
        servicioAuth.activarCuenta(correo, codigoOtp, telefono, request.getRemoteAddr());
        
        return ResponseEntity.ok(ResultadoApi.exito("OK", "Cuenta activada correctamente. Ya puede iniciar sesión."));
    }

    /**
     * Activa la cuenta de un usuario recién registrado por su ID.
     * Invocado internamente por ms-mensajeria tras una validación OTP exitosa.
     */
    @PutMapping("/activar/{usuarioId}")
    @Operation(summary = "Activar Cuenta por ID (Interno)", description = "Activa directamente la cuenta de un usuario por su UUID. Invocado principalmente de forma interna por ms-mensajeria.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cuenta activada exitosamente."),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado.")
    })
    public ResponseEntity<ResultadoApi<String>> activarCuentaPorId(
            @PathVariable @Parameter(description = "UUID del usuario.", example = "d3b07384-d113-4a0b-8083-d922a901ba8d") UUID usuarioId,
            @RequestParam(required = false) @Parameter(description = "Teléfono verificado a sincronizar.", example = "+51999999999") String telefono) {
        
        log.info("[API-INTERNAL] Intento de activación por ID para: {}", usuarioId);
        servicioAuth.activarCuentaPorId(usuarioId, telefono);
        
        return ResponseEntity.ok(ResultadoApi.exito("OK", "Cuenta activada correctamente."));
    }

    /**
     * Solicita un nuevo código OTP para la activación de cuenta.
     */
    @PostMapping("/solicitar-otp")
    @Operation(summary = "Solicitar Nuevo OTP", description = "Genera y reenvía un nuevo código OTP de activación al medio de contacto especificado (EMAIL, SMS o WHATSAPP) en caso de que el anterior haya expirado o no haya sido recibido.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Código OTP enviado exitosamente al medio de contacto."),
            @ApiResponse(responseCode = "400", description = "El canal seleccionado no es válido o hay demasiadas solicitudes pendientes (Rate Limit de OTP)."),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado.")
    })
    public ResponseEntity<ResultadoApi<String>> solicitarOtpActivacion(
            @Valid @RequestBody SolicitudReenvioOtp solicitud) {
        
        log.info("[API] Solicitud de OTP de activación para email: {}", solicitud.email());
        servicioAuth.solicitarOtpActivacion(solicitud);
        
        return ResponseEntity.ok(ResultadoApi.exito("OTP_ENVIADO", "El código ha sido enviado a su medio de contacto."));
    }

    /**
     * Realiza el login del usuario y retorna el token JWT.
     */
    @PostMapping("/login")
    @Operation(summary = "Iniciar Sesión (Autenticación)", description = "Valida las credenciales de correo y contraseña de un usuario en el sistema. Retorna un token de acceso JWT firmado (HS384) y un refresh token, además de los datos de su plan de suscripción actual.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Autenticación exitosa. Se retorna el token JWT y el perfil de acceso."),
            @ApiResponse(responseCode = "401", description = "Credenciales incorrectas o IP bloqueada temporalmente por fuerza bruta."),
            @ApiResponse(responseCode = "403", description = "El usuario existe pero su cuenta no se encuentra activada (requiere verificación OTP).")
    })
    public ResponseEntity<ResultadoApi<RespuestaAutenticacion>> login(
            @Valid @RequestBody SolicitudLogin solicitud,
            HttpServletRequest request) {
        
        log.info("[API] Intento de login para: {}", solicitud.correo());
        RespuestaAutenticacion respuesta = servicioAuth.login(solicitud, request.getRemoteAddr());
        
        return ResponseEntity.ok(ResultadoApi.exito(respuesta, "Autenticación exitosa."));
    }

    /**
     * Renueva el Access Token utilizando un Refresh Token válido.
     */
    @PostMapping("/refrescar-token")
    @Operation(summary = "Renovar Access Token", description = "Recibe un Refresh Token válido y genera un nuevo Access Token JWT firmado para prolongar la sesión activa del usuario.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Token renovado exitosamente. Se retorna un nuevo Access Token."),
            @ApiResponse(responseCode = "400", description = "Refresh Token inválido, expirado o malformado.")
    })
    public ResponseEntity<ResultadoApi<RespuestaAutenticacion>> refrescarToken(
            @Valid @RequestBody SolicitudRefreshToken solicitud,
            HttpServletRequest request) {
        
        log.info("[API] Solicitud de refresco de token");
        RespuestaAutenticacion respuesta = servicioAuth.refrescarToken(solicitud, request.getRemoteAddr());
        
        return ResponseEntity.ok(ResultadoApi.exito(respuesta, "Token renovado exitosamente."));
    }

    /**
     * Cierra la sesión del usuario actual e invalida el token.
     */
    @PostMapping("/logout")
    @Operation(summary = "Cerrar Sesión (Logout)", description = "Invalida el token JWT activo del usuario guardándolo en la blacklist de Redis con un TTL equivalente al tiempo de expiración restante.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Sesión cerrada correctamente. El token ha sido invalidado."),
            @ApiResponse(responseCode = "401", description = "No autorizado. Token inválido o ausente.")
    })
    public ResponseEntity<ResultadoApi<String>> logout(Authentication authentication, HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        String token = (authHeader != null && authHeader.startsWith("Bearer ")) ? authHeader.substring(7) : null;

        if (authentication != null && authentication.getPrincipal() instanceof Usuario usuario) {
            servicioAuth.registrarLogout(usuario.getId(), token, request.getRemoteAddr());
        }
        return ResponseEntity.ok(ResultadoApi.exito("Sesión cerrada", "Sesión cerrada correctamente. El token ha sido invalidado."));
    }

    /**
     * Registra un nuevo usuario en el sistema.
     */
    @PostMapping("/registrar")
    @Operation(summary = "Registrar Nuevo Usuario", description = "Crea una nueva cuenta de usuario en estado inactivo con el rol base ROLE_FREE. Genera y envía un código OTP de activación a través de ms-mensajeria.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Usuario creado exitosamente. Se retorna el UUID de la cuenta."),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos o formato de contraseña incorrecto."),
            @ApiResponse(responseCode = "409", description = "Conflicto por duplicidad: el nombre de usuario o correo electrónico ya están registrados.")
    })
    public ResponseEntity<ResultadoApi<UUID>> registrar(
            @Valid @RequestBody SolicitudRegistro solicitud, 
            HttpServletRequest request) {
        
        log.info("[API] Solicitud de registro para usuario: {}", solicitud.nombreUsuario());
        UUID usuarioId = servicioAuth.registrar(solicitud, request.getRemoteAddr());
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ResultadoApi.exito(usuarioId, "Registro exitoso. Revise su correo para activar la cuenta."));
    }

    /**
     * Inicia el flujo de recuperación de contraseña.
     */
    @PostMapping("/recuperar-solicitar")
    @Operation(summary = "Solicitar Recuperación de Contraseña", description = "Inicia el flujo de recuperación de contraseña enviando un código OTP al correo del usuario si este existe y está activo.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Solicitud procesada. Si el correo existe, recibirá el código OTP en breve.")
    })
    public ResponseEntity<ResultadoApi<String>> solicitarRecuperacion(
            @Valid @RequestBody SolicitudRecuperacion solicitud) {
        
        log.info("[API] Solicitud de recuperación para: {}", solicitud.correo());
        servicioAuth.iniciarRecuperacion(solicitud);
        
        return ResponseEntity.ok(ResultadoApi.exito("SOLICITUD_PROCESADA", 
            "Si el correo ingresado existe y está activo, recibirá un código de verificación en breve."));
    }

    /**
     * Establece una nueva contraseña tras validar el código de recuperación.
     */
    @PostMapping("/recuperar-confirmar")
    @Operation(summary = "Confirmar Recuperación de Contraseña", description = "Establece una nueva contraseña tras validar el código OTP de recuperación. El usuario debe enviar su correo, el OTP recibido y la nueva contraseña.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Contraseña restablecida correctamente. Ya puede iniciar sesión."),
            @ApiResponse(responseCode = "400", description = "Código OTP inválido o expirado.")
    })
    public ResponseEntity<ResultadoApi<String>> resetearPassword(
            @Valid @RequestBody SolicitudRestablecerPassword solicitud) {
        
        log.info("[API] Confirmación de recuperación de contraseña para: {}", solicitud.correo());
        servicioAuth.restablecerPassword(solicitud);
        
        return ResponseEntity.ok(ResultadoApi.exito("PASSWORD_RESETEADO", "Su contraseña ha sido restablecida correctamente."));
    }

    /**
     * Permite a un usuario autenticado cambiar su contraseña.
     */
    @PutMapping("/cambiar-password")
    @Operation(summary = "Cambiar Contraseña", description = "Permite a un usuario autenticado cambiar su contraseña actual por una nueva validando la coincidencia de las contraseñas ingresadas.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Contraseña actualizada correctamente."),
            @ApiResponse(responseCode = "400", description = "La contraseña actual no coincide o la nueva contraseña no cumple con los requisitos de seguridad."),
            @ApiResponse(responseCode = "401", description = "No autorizado.")
    })
    public ResponseEntity<ResultadoApi<String>> cambiarPassword(
            @Valid @RequestBody SolicitudCambioPassword solicitud,
            Authentication authentication,
            HttpServletRequest request) {

        UUID usuarioId = ((Usuario) authentication.getPrincipal()).getId();
        servicioAuth.cambiarPassword(usuarioId, solicitud, request.getRemoteAddr());

        return ResponseEntity.ok(ResultadoApi.exito("PASSWORD_ACTUALIZADO", "La contraseña ha sido actualizada correctamente."));
    }

    /**
     * Desactiva lógicamente la cuenta del usuario autenticado.
     */
    @DeleteMapping("/mi-cuenta")
    @Operation(summary = "Desactivar/Eliminar Cuenta", description = "Desactiva de forma lógica la cuenta del usuario autenticado en la base de datos, impidiendo futuros inicios de sesión.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cuenta desactivada exitosamente."),
            @ApiResponse(responseCode = "401", description = "No autorizado.")
    })
    public ResponseEntity<ResultadoApi<String>> eliminarCuenta(
            Authentication authentication,
            HttpServletRequest request) {

        Usuario usuario = (Usuario) authentication.getPrincipal();
        servicioAuth.eliminarCuenta(usuario.getId(), request.getRemoteAddr());

        return ResponseEntity.ok(ResultadoApi.exito("CUENTA_ELIMINADA", "Su cuenta ha sido desactivada exitosamente."));
    }
}
