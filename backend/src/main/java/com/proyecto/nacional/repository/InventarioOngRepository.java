package com.proyecto.nacional.repository;

import com.proyecto.nacional.model.InventarioOng;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface InventarioOngRepository extends JpaRepository<InventarioOng, Long> {

    @Query("select i from InventarioOng i join fetch i.recurso where i.ong.id = :ongId order by i.recurso.id")
    List<InventarioOng> findByOngId(@Param("ongId") Long ongId);

    @Query("select i from InventarioOng i join fetch i.recurso where i.ong.id in :ongIds order by i.recurso.id")
    List<InventarioOng> findByOngIdIn(@Param("ongIds") Collection<Long> ongIds);

    Optional<InventarioOng> findByOngIdAndRecursoId(Long ongId, Long recursoId);

    void deleteByOngId(Long ongId);
}
