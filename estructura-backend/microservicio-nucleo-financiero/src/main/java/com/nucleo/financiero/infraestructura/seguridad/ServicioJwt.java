package com.nucleo.financiero.infraestructura.seguridad;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import javax.crypto.SecretKey;
import java.util.*;
import java.util.function.Function;

@Service
@Slf4j
public class ServicioJwt {

    @Value("${application.security.jwt.secret-key}")
    private String claveSecreta;

    public String extraerNombreUsuario(String token) {
        return extraerClaim(token, Claims::getSubject);
    }

    @SuppressWarnings("unchecked")
    public List<String> extraerRoles(String token) {
        return extraerClaim(token, claims -> (List<String>) claims.get("roles"));
    }

    public <T> T extraerClaim(String token, Function<Claims, T> resolvedor) {
        return resolvedor.apply(extraerTodosLosClaims(token));
    }

    private Claims extraerTodosLosClaims(String token) {
        return Jwts.parser()
                .verifyWith(obtenerClaveFirma())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean estaExpirado(String token) {
        return extraerClaim(token, Claims::getExpiration).before(new Date());
    }

    private SecretKey obtenerClaveFirma() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(claveSecreta));
    }
}
