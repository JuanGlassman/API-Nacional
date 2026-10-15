package com.proyecto.nacional.config;

import com.proyecto.nacional.security.CuentaUserDetailsService;
import com.proyecto.nacional.security.JwtAuthenticationFilter;
import com.proyecto.nacional.security.JwtService;
import com.proyecto.nacional.security.ManejadorErroresSeguridad;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * API stateless protegida con JWT. Todo requiere token salvo el login, el health check
 * (lo usa Render) y Swagger.
 *
 * Los permisos por rol se declaran en cada endpoint con @PreAuthorize:
 * - ADMIN: gestión, aprobación y habilitación de ONGs y cuentas
 * - ONG: sus propios datos, certificaciones y recursos
 * - SISTEMA_CLIENTE (Bonita): validación, compromiso y liberación de recursos
 * - Cualquier cuenta autenticada: catálogos de consulta
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtService jwtService;
    private final CuentaUserDetailsService cuentaUserDetailsService;
    private final ManejadorErroresSeguridad manejadorErroresSeguridad;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/login").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers("/error").permitAll()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(manejadorErroresSeguridad)
                        .accessDeniedHandler(manejadorErroresSeguridad)
                )
                .addFilterBefore(new JwtAuthenticationFilter(jwtService, cuentaUserDetailsService),
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // Lo usa el login para validar usuario y contraseña contra la tabla de cuentas
    @Bean
    public AuthenticationManager authenticationManager(PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(cuentaUserDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(provider);
    }
}
