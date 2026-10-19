package com.proyecto.nacional.repository;

import com.proyecto.nacional.model.Oferta;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.List;
import java.util.Optional;

public interface OfertaRepository extends JpaRepository<Oferta, Long> {
    List<Oferta> findByEmergenciaIdAndLoteId(Long emergenciaId, Long loteId);

    // Con lock: las operaciones de reserva sobre una misma oferta se hacen de a una
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Oferta> findByEmergenciaIdAndLoteIdAndOfertaId(Long emergenciaId, Long loteId, Long ofertaId);
}
