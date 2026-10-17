package com.proyecto.nacional.repository;

import com.proyecto.nacional.model.Oferta;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OfertaRepository extends JpaRepository<Oferta, Long> {
    List<Oferta> findByEmergenciaIdAndLoteId(Long emergenciaId, Long loteId);
}
