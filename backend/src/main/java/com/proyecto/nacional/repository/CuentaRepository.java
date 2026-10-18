package com.proyecto.nacional.repository;

import com.proyecto.nacional.model.Cuenta;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CuentaRepository extends JpaRepository<Cuenta, Long> {

    Optional<Cuenta> findByUsername(String username);

    boolean existsByUsername(String username);
}
