package com.usuario.infraestructura.seguridad;

import com.usuario.dominio.entidades.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Servicio responsable de toda la lógica JWT: generación, validación y
 * extracción de claims.
 */
@Service
@Slf4j
public class ServicioJwt {

    @Value("${application.security.jwt.secret-key}")
    private String claveSecreta;

    @Value("${application.security.jwt.expiration}")
    private long expiracionJwt;

    // -------------------------------------------------------------------------
    // Generación de tokens
    // -------------------------------------------------------------------------
    public String generarToken(UserDetails userDetails) {
        Map<String, Object> claimsExtra = new HashMap<>();

        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());
        if (userDetails instanceof Usuario usuario) {
            // Metemos toda la información que necesitas en los claims
            claimsExtra.put("usuarioId", usuario.getId().toString());
            claimsExtra.put("nombre", usuario.getNombreUsuario());
            claimsExtra.put("correo", usuario.getCorreo());
        }
        claimsExtra.put("roles", roles);

        return construirToken(claimsExtra, userDetails, expiracionJwt);
    }

    private String construirToken(Map<String, Object> claimsExtra,
            UserDetails usuario,
            long expiracion) {
        return Jwts.builder()
                .claims(claimsExtra)
                .subject(usuario.getUsername())
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expiracion))
                .signWith(obtenerClaveFirma())
                .compact();
    }
    // -------------------------------------------------------------------------
    // Extracción de claims
    // -------------------------------------------------------------------------

    public String extraerCorreoUsuario(String token) {
        return extraerClaim(token, Claims::getSubject);
    }

    @SuppressWarnings("unchecked")
    public List<String> extraerRoles(String token) {
        return extraerClaim(token, claims -> (List<String>) claims.get("roles"));
    }

    public <T> T extraerClaim(String token, Function<Claims, T> resolvedorClaims) {
        final Claims claims = extraerTodosLosClaims(token);
        return resolvedorClaims.apply(claims);
    }

    private Claims extraerTodosLosClaims(String token) {
        return Jwts.parser()
                .verifyWith(obtenerClaveFirma())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
    // -------------------------------------------------------------------------
    // Validación
    // -------------------------------------------------------------------------

    public boolean esTokenValido(String token, UserDetails usuario) {
        try {
            final String correo = extraerCorreoUsuario(token);
            return correo.equals(usuario.getUsername()) && !estaExpirado(token);
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Token JWT inválido: {}", e.getMessage());
            return false;
        }
    }

    public boolean estaExpirado(String token) {
        return obtenerExpiracion(token).before(new Date());
    }

    private Date obtenerExpiracion(String token) {
        return extraerClaim(token, Claims::getExpiration);
    }

    public long obtenerExpiracionJwt() {
        return expiracionJwt;
    }

    // -------------------------------------------------------------------------
    // Clave de firma
    // -------------------------------------------------------------------------
    private SecretKey obtenerClaveFirma() {
        byte[] keyBytes = Decoders.BASE64.decode(claveSecreta);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
