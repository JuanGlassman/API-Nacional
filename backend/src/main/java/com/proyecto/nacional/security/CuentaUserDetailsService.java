package com.proyecto.nacional.security;

import com.proyecto.nacional.model.Cuenta;
import com.proyecto.nacional.repository.CuentaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

// Adapta una Cuenta a lo que Spring Security necesita para autenticar y autorizar
@Service
@RequiredArgsConstructor
public class CuentaUserDetailsService implements UserDetailsService {

    private final CuentaRepository cuentaRepository;

    @Override
    public UserDetails loadUserByUsername(String username) {
        Cuenta cuenta = cuentaRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("No existe la cuenta " + username));

        // roles() agrega el prefijo ROLE_, así funciona hasRole('ADMIN') en @PreAuthorize
        return User.withUsername(cuenta.getUsername())
                .password(cuenta.getPasswordHash())
                .roles(cuenta.getRol().name())
                .disabled(!cuenta.isHabilitada())
                .build();
    }
}
