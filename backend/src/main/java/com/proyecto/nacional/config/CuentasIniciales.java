package com.proyecto.nacional.config;

import com.proyecto.nacional.model.Cuenta;
import com.proyecto.nacional.model.Rol;
import com.proyecto.nacional.repository.CuentaRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Crea al arrancar las cuentas fijas del sistema si todavía no existen: el administrador y la
 * cuenta que usa Bonita. Las contraseñas vienen de variables de entorno para no dejarlas en el
 * repositorio; si falta alguna, esa cuenta no se crea.
 */
@Slf4j
@Component
public class CuentasIniciales implements CommandLineRunner {

    private final CuentaRepository cuentaRepository;
    private final PasswordEncoder passwordEncoder;
    private final String passwordAdmin;
    private final String passwordBonita;

    public CuentasIniciales(CuentaRepository cuentaRepository,
                            PasswordEncoder passwordEncoder,
                            @Value("${cuentas.admin.password:}") String passwordAdmin,
                            @Value("${cuentas.bonita.password:}") String passwordBonita) {
        this.cuentaRepository = cuentaRepository;
        this.passwordEncoder = passwordEncoder;
        this.passwordAdmin = passwordAdmin;
        this.passwordBonita = passwordBonita;
    }

    @Override
    public void run(String... args) {
        crearSiNoExiste("admin", passwordAdmin, Rol.ADMIN, "ADMIN_PASSWORD");
        crearSiNoExiste("bonita", passwordBonita, Rol.SISTEMA_CLIENTE, "BONITA_PASSWORD");
    }

    private void crearSiNoExiste(String username, String password, Rol rol, String variable) {
        if (cuentaRepository.existsByUsername(username)) {
            return;
        }
        if (password.isBlank()) {
            log.warn("No se creó la cuenta '{}': falta la variable de entorno {}", username, variable);
            return;
        }

        Cuenta cuenta = new Cuenta();
        cuenta.setUsername(username);
        cuenta.setPasswordHash(passwordEncoder.encode(password));
        cuenta.setRol(rol);
        cuenta.setHabilitada(true);
        cuentaRepository.save(cuenta);
        log.info("Cuenta inicial '{}' creada con rol {}", username, rol);
    }
}
