package com.financiero.saas.gateway.seguridad;

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
import java.util.function.Function;

@Slf4j
@Service
public class ServicioJwtGateway {

    @Value("${app.security.jwt.secret-key}")
    private String claveSecreta;

    /**
     * Valida si el token es legítimo, no ha sido manipulado y tiene tiempo de
     * vida.
     * @param token
     * @return 
     */
    public boolean esTokenValido(String token) {
        try {
            Claims claims = extraerTodosLosClaims(token);
            // Verificamos que no haya expirado
            boolean noExpirado = claims.getExpiration().after(new Date());

            if (!noExpirado) {
                log.warn("Intento de acceso con token expirado de: {}", claims.getSubject());
            }

            return noExpirado;
        } catch (JwtException e) {
            log.error("Fallo en la validación de firma JWT: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("Error no controlado en validación JWT: {}", e.getMessage());
            return false;
        }
    }

    // --- EXTRACCIÓN DE DATOS ---
    public String extraerNombreUsuario(String token) {
        return extraerClaim(token, Claims::getSubject);
    }

    public String extraerUsuarioId(String token) {
        // Extraemos el UUID que definiste en el microservicio de usuario
        return extraerClaim(token, claims -> claims.get("usuarioId", String.class));
    }

    @SuppressWarnings("unchecked")
    public List<String> extraerRoles(String token) {
        return extraerClaim(token, claims -> (List<String>) claims.get("roles"));
    }

    // --- UTILIDADES INTERNAS ---
    public <T> T extraerClaim(String token, Function<Claims, T> resolver) {
        final Claims claims = extraerTodosLosClaims(token);
        return resolver.apply(claims);
    }

    private Claims extraerTodosLosClaims(String token) {
        return Jwts.parser()
                .verifyWith(obtenerLlaveFirma())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey obtenerLlaveFirma() {
        // Usamos decodificación Base64 para mayor seguridad
        byte[] keyBytes = Decoders.BASE64.decode(claveSecreta);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
