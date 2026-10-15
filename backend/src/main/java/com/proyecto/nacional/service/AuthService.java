package com.proyecto.nacional.service;

import com.proyecto.nacional.dto.auth.LoginRequest;
import com.proyecto.nacional.dto.auth.LoginResponse;
import com.proyecto.nacional.model.Cuenta;
import com.proyecto.nacional.repository.CuentaRepository;
import com.proyecto.nacional.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String TIPO_TOKEN = "Bearer";

    private final AuthenticationManager authenticationManager;
    private final CuentaRepository cuentaRepository;
    private final JwtService jwtService;

    // Si las credenciales no son válidas o la cuenta está deshabilitada, authenticate lanza
    // una AuthenticationException que el GlobalExceptionHandler convierte en 401
    public LoginResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(request.getUsername(), request.getPassword()));

        Cuenta cuenta = cuentaRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BadCredentialsException("La cuenta dejó de existir durante el login"));

        return new LoginResponse(
                jwtService.generarToken(cuenta),
                TIPO_TOKEN,
                jwtService.getVigenciaEnSegundos(),
                cuenta.getRol().name()
        );
    }
}
