package com.proyecto.nacional.repository;

import com.proyecto.nacional.model.InventarioOng;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
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

    // SELECT ... FOR UPDATE: mientras dure la transacción nadie más reserva ni cambia el stock de estas filas.
    // Siempre en orden de id para que dos transacciones que toman varias filas no se bloqueen entre sí.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from InventarioOng i where i.ong.id in :ongIds and i.recurso.id in :recursoIds order by i.id")
    List<InventarioOng> bloquearPorOngYRecurso(@Param("ongIds") Collection<Long> ongIds,
                                               @Param("recursoIds") Collection<Long> recursoIds);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from InventarioOng i where i.ong.id = :ongId and i.recurso.id = :recursoId")
    Optional<InventarioOng> bloquearPorOngYRecurso(@Param("ongId") Long ongId, @Param("recursoId") Long recursoId);

    void deleteByOngId(Long ongId);
}
