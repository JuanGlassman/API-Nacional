package com.proyecto.nacional.security;

import com.proyecto.nacional.model.Cuenta;
import com.proyecto.nacional.model.Rol;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import io.jsonwebtoken.security.WeakKeyException;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SECRETO = "secreto-de-prueba-para-los-tests-de-jwt";

    private final JwtService jwtService = new JwtService(SECRETO, 60);

    @Test
    void generaUnTokenConUsuarioRolYVencimiento() {
        String token = jwtService.generarToken(cuenta("bonita", Rol.SISTEMA_CLIENTE));

        Claims claims = jwtService.extraerClaims(token);

        assertThat(claims.getSubject()).isEqualTo("bonita");
        assertThat(claims.get(JwtService.CLAIM_ROL, String.class)).isEqualTo("SISTEMA_CLIENTE");
        assertThat(claims.getExpiration()).isAfter(new Date());
        assertThat(jwtService.getVigenciaEnSegundos()).isEqualTo(3600);
    }

    @Test
    void rechazaUnTokenFirmadoConOtroSecreto() {
        JwtService otroServicio = new JwtService("otro-secreto-distinto-de-al-menos-32-bytes", 60);
        String token = otroServicio.generarToken(cuenta("admin", Rol.ADMIN));

        assertThatThrownBy(() -> jwtService.extraerClaims(token)).isInstanceOf(SignatureException.class);
    }

    @Test
    void rechazaUnTokenVencido() {
        Instant haceDosHoras = Instant.now().minus(2, ChronoUnit.HOURS);
        String token = Jwts.builder()
                .subject("admin")
                .issuedAt(Date.from(haceDosHoras))
                .expiration(Date.from(haceDosHoras.plus(1, ChronoUnit.HOURS)))
                .signWith(Keys.hmacShaKeyFor(SECRETO.getBytes(StandardCharsets.UTF_8)))
                .compact();

        assertThatThrownBy(() -> jwtService.extraerClaims(token)).isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    void noArrancaConUnSecretoCorto() {
        assertThatThrownBy(() -> new JwtService("corto", 60)).isInstanceOf(WeakKeyException.class);
    }

    private Cuenta cuenta(String username, Rol rol) {
        Cuenta cuenta = new Cuenta();
        cuenta.setUsername(username);
        cuenta.setRol(rol);
        return cuenta;
    }
}
