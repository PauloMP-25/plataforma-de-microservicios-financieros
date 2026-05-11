package com.libreria.comun.seguridad;

import com.libreria.comun.excepciones.ExcepcionNoAutorizado;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import javax.crypto.SecretKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

/**
 * Servicio centralizado para la gestión de tokens JSON Web Token (JWT).
 * <p>
 * Proporciona funcionalidades para la generación, parseo y validación de
 * tokens, asegurando la interoperabilidad con servicios externos (como ms-ia en
 * Python) mediante el uso de una clave secreta compartida.
 * </p>
 *
 * @author Paulo Moron
 * @version 1.1.0
 */
@Slf4j
@Service
public class ServicioJwt {

    /**
     * Clave secreta proporcionada por variables de entorno (Base64).
     */
    @Value("${luka.jwt.clave-secreta}")
    private String claveSecreta;

    /**
     * Tiempo de vida del token en milisegundos (24h por defecto).
     */
    @Value("${luka.jwt.expiracion-ms:86400000}")
    private long expiracionMs;

    /**
     * Tiempo de vida del token de refresco (7 días por defecto).
     */
    @Value("${luka.jwt.refresh-expiracion-ms:604800000}")
    private long expiracionRefreshMs;

    /**
     * @return Tiempo de vida del token en milisegundos.
     */
    public long obtenerExpiracionMs() {
        return expiracionMs;
    }

    /**
     * @return Tiempo de vida del token de refresco en milisegundos.
     */
    public long obtenerExpiracionRefreshMs() {
        return expiracionRefreshMs;
    }

    /**
     * Genera la clave de firma compatible con algoritmos HMAC-SHA decodificando
     * la clave en Base64.
     *
     * @return {@link SecretKey} para firma y verificación.
     */
    private SecretKey obtenerClaveFirma() {
        byte[] keyBytes = Decoders.BASE64.decode(claveSecreta);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Genera un token JWT firmado para un usuario autenticado.
     *
     * @param userDetails Entidad que representa al usuario autenticado.
     * @param claimsExtra Mapa con datos adicionales.
     * @return String que representa el token JWT compacto.
     */
    public String generarToken(UserDetails userDetails, Map<String, Object> claimsExtra) {
        return generarTokenConExpiracion(userDetails, claimsExtra, expiracionMs);
    }

    /**
     * Genera un Refresh Token con mayor tiempo de vida y sin roles.
     *
     * @param userDetails Datos del usuario.
     * @return Token JWT de larga duración.
     */
    public String generarRefreshToken(UserDetails userDetails) {
        return generarTokenConExpiracion(userDetails, new HashMap<>(), expiracionRefreshMs);
    }

    private String generarTokenConExpiracion(UserDetails userDetails, Map<String, Object> claimsExtra, long durationMs) {
        Map<String, Object> claims = new HashMap<>(claimsExtra);

        if (durationMs == expiracionMs) {
            List<String> roles = userDetails.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .toList();
            claims.put("roles", roles);
        }

        return Jwts.builder()
                .claims(claims)
                .subject(userDetails.getUsername())
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + durationMs))
                .signWith(obtenerClaveFirma())
                .compact();
    }

    /**
     * Extrae el Subject (usualmente el correo electrónico) contenido en el
     * token.
     *
     * @param token Token JWT Bearer.
     * @return Subject del token.
     */
    public String extraerSubject(String token) {
        return extraerClaim(token, Claims::getSubject);
    }

    /**
     * Extrae el Identificador de Usuario (UUID) contenido en el claim
     * personalizado "usuarioId".
     *
     * @param token Token JWT Bearer.
     * @return UUID del usuario o null si no está presente.
     */
    public UUID extraerUsuarioId(String token) {
        String id = extraerClaim(token, claims -> claims.get("usuarioId", String.class));
        return id != null ? UUID.fromString(id) : null;
    }

    /**
     * Método genérico para extraer cualquier información (Claim) del token.
     *
     * @param <T> Tipo de dato esperado del Claim.
     * @param token Token JWT.
     * @param claimsResolver Función lógica para extraer un claim específico.
     * @return Valor del claim procesado por el resolvedor.
     */
    public <T> T extraerClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extraerTodosLosClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Realiza el parseo y verificación de la firma del token utilizando la
     * clave secreta.
     *
     * @param token Token JWT.
     * @return Objeto {@link Claims} con la carga útil.
     * @throws ExcepcionNoAutorizado Si el token es inválido o ha expirado.
     */
    private Claims extraerTodosLosClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(obtenerClaveFirma())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            log.warn("Intento de acceso con token expirado");
            throw new ExcepcionNoAutorizado("TOKEN_EXPIRADO");
        } catch (JwtException e) {
            log.error("Fallo en la validación de firma JWT");
            throw new ExcepcionNoAutorizado("TOKEN_INVALIDO");
        }
    }

    /**
     * Extrae la lista de roles del usuario del token.
     *
     * @param token Token JWT Bearer.
     * @return Lista de Strings con los nombres de los roles.
     */
    @SuppressWarnings("unchecked")
    public List<String> extraerRoles(String token) {
        return extraerTodosLosClaims(token).get("roles", List.class);
    }

    /**
     * Valida el token comparando el subject con el nombre de usuario de
     * {@link UserDetails}.
     *
     * @param token Token JWT Bearer.
     * @param userDetails Información del usuario cargada en el sistema.
     * @return true si el token pertenece al usuario y no ha expirado.
     */
    public boolean esTokenValido(String token, UserDetails userDetails) {
        final String username = extraerSubject(token);
        return (username.equals(userDetails.getUsername()) && !estaExpirado(token));
    }

    /**
     * Validación simple de integridad y expiración del token.
     *
     * @param token Token JWT Bearer.
     * @return true si el token es estructuralmente válido y vigente.
     */
    public boolean esTokenValido(String token) {
        return !estaExpirado(token);
    }

    /**
     * Comprueba si el token ha superado su fecha de expiración.
     *
     * @param token Token JWT.
     * @return true si la fecha actual es posterior a la de expiración.
     */
    private boolean estaExpirado(String token) {
        return extraerClaim(token, Claims::getExpiration).before(new Date());
    }
}
