package com.cliente.infraestructura.seguridad;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

/**
 * Solo VALIDA y EXTRAE claims del JWT emitido por el microservicio IAM.
 * Este servicio NO genera tokens.
 */
@Service
@Slf4j
public class ServicioJwt {

    @Value("${application.security.jwt.secret-key}")
    private String claveSecreta;

    // ── Extracción de claims ──────────────────────────────────────────────────

    public String extraerNombreUsuario(String token) {
        return extraerClaim(token, Claims::getSubject);
    }

    public UUID extraerUsuarioId(String token) {
        // El IAM pone el subject como nombreUsuario, no UUID.
        // Extraemos el claim "usuarioId" que debe añadirse en el IAM (ver nota abajo)
        String raw = extraerClaim(token, claims -> claims.get("usuarioId", String.class));
        if (raw == null) return null;
        return UUID.fromString(raw);
    }

    @SuppressWarnings("unchecked")
    public List<String> extraerRoles(String token) {
        return extraerClaim(token, claims -> (List<String>) claims.get("roles"));
    }

    public <T> T extraerClaim(String token, Function<Claims, T> resolver) {
        return resolver.apply(extraerTodosLosClaims(token));
    }

    // ── Validación ────────────────────────────────────────────────────────────

    public boolean esTokenValido(String token) {
        try {
            return !estaExpirado(token);
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Token JWT inválido: {}", e.getMessage());
            return false;
        }
    }

    public boolean estaExpirado(String token) {
        return extraerClaim(token, Claims::getExpiration).before(new Date());
    }

    // ── Privados ──────────────────────────────────────────────────────────────

    private Claims extraerTodosLosClaims(String token) {
        return Jwts.parser()
                .verifyWith(obtenerClaveFirma())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey obtenerClaveFirma() {
        byte[] keyBytes = Decoders.BASE64.decode(claveSecreta);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}