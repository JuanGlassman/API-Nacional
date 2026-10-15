package com.proyecto.nacional.security;

import com.proyecto.nacional.model.Cuenta;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

// Emite y valida los JWT (HS256) que usan los clientes como Bearer Token
@Service
public class JwtService {

    public static final String CLAIM_ROL = "rol";

    private final SecretKey clave;
    private final Duration vigencia;

    // Keys.hmacShaKeyFor falla al arrancar si el secreto tiene menos de 32 bytes
    public JwtService(@Value("${jwt.secret}") String secreto,
                      @Value("${jwt.expiration-minutes}") long minutosDeVigencia) {
        this.clave = Keys.hmacShaKeyFor(secreto.getBytes(StandardCharsets.UTF_8));
        this.vigencia = Duration.ofMinutes(minutosDeVigencia);
    }

    public String generarToken(Cuenta cuenta) {
        Instant ahora = Instant.now();
        return Jwts.builder()
                .subject(cuenta.getUsername())
                .claim(CLAIM_ROL, cuenta.getRol().name())
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(ahora.plus(vigencia)))
                .signWith(clave)
                .compact();
    }

    /**
     * Valida firma y vencimiento del token y devuelve sus claims.
     *
     * @throws JwtException si el token está mal formado, adulterado o vencido
     */
    public Claims extraerClaims(String token) {
        return Jwts.parser()
                .verifyWith(clave)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public long getVigenciaEnSegundos() {
        return vigencia.toSeconds();
    }
}
