package com.proyecto.nacional.repository;

import com.proyecto.nacional.model.Cuenta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CuentaRepository extends JpaRepository<Cuenta, Long> {

    Optional<Cuenta> findByUsername(String username);

    boolean existsByUsername(String username);

    boolean existsByOngId(Long ongId);

    // Id de la ONG de la cuenta, sin cargar la entidad (vacío si la cuenta no tiene ONG)
    @Query("select c.ong.id from Cuenta c where c.username = :username")
    Optional<Long> findOngIdByUsername(@Param("username") String username);
}
