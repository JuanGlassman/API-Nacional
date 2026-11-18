package com.proyecto.nacional.repository;

import com.proyecto.nacional.model.Oferta;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OfertaRepository extends JpaRepository<Oferta, Long> {
    List<Oferta> findByEmergenciaIdAndLoteId(Long emergenciaId, Long loteId);

    // Todas las ofertas de un lote con lock, en orden de id: la validación del lote se hace de a una
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from Oferta o where o.emergenciaId = :emergenciaId and o.loteId = :loteId order by o.id")
    List<Oferta> bloquearPorLote(@Param("emergenciaId") Long emergenciaId, @Param("loteId") Long loteId);

    // Con lock: las operaciones de reserva sobre una misma oferta se hacen de a una
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Oferta> findByEmergenciaIdAndLoteIdAndOfertaId(Long emergenciaId, Long loteId, Long ofertaId);
}
