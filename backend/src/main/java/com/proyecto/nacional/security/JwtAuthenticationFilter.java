package com.proyecto.nacional.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Lee el header "Authorization: Bearer <token>", valida el JWT y deja la cuenta autenticada
 * en el SecurityContext. Si no hay token o es inválido sigue sin autenticar: el rechazo (401)
 * lo decide la configuración de seguridad según la ruta.
 *
 * No es un @Component a propósito: Spring Boot registraría el filtro también fuera de la
 * cadena de seguridad. Se instancia en SecurityConfig.
 */
@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String PREFIJO_BEARER = "Bearer ";

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith(PREFIJO_BEARER)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(PREFIJO_BEARER.length());
        try {
            String username = jwtService.extraerClaims(token).getSubject();
            // Se recarga la cuenta para que una cuenta deshabilitada pierda el acceso
            // aunque todavía tenga un token vigente
            UserDetails cuenta = userDetailsService.loadUserByUsername(username);
            if (cuenta.isEnabled()) {
                UsernamePasswordAuthenticationToken autenticacion =
                        UsernamePasswordAuthenticationToken.authenticated(cuenta, null, cuenta.getAuthorities());
                autenticacion.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(autenticacion);
            }
        } catch (JwtException | IllegalArgumentException | UsernameNotFoundException ex) {
            log.debug("{} {} -> token rechazado: {}", request.getMethod(), request.getRequestURI(), ex.getMessage());
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}
