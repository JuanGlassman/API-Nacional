package com.proyecto.nacional.repository;

import com.proyecto.nacional.model.Reserva;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ReservaRepository extends JpaRepository<Reserva, Long> {

    List<Reserva> findByEmergenciaIdAndLoteIdAndOfertaIdOrderByIdAsc(Long emergenciaId, Long loteId, Long ofertaId);

    // Cantidad que hoy tienen tomada las reservas de un recurso de una ONG: las comprometidas, y las
    // bloqueadas que todavía no vencieron
    @Query("""
            select coalesce(sum(r.cantidad), 0) from Reserva r
            where r.ong.id = :ongId and r.recurso.id = :recursoId
              and (r.estado = com.proyecto.nacional.model.EstadoReserva.COMPROMETIDA
                   or (r.estado = com.proyecto.nacional.model.EstadoReserva.BLOQUEADA and r.expiraEn > :ahora))
            """)
    long sumarActivas(@Param("ongId") Long ongId, @Param("recursoId") Long recursoId,
                      @Param("ahora") LocalDateTime ahora);

    @Query("""
            select count(r) > 0 from Reserva r
            where r.ong.id = :ongId
              and (r.estado = com.proyecto.nacional.model.EstadoReserva.COMPROMETIDA
                   or (r.estado = com.proyecto.nacional.model.EstadoReserva.BLOQUEADA and r.expiraEn > :ahora))
            """)
    boolean existenActivasDeOng(@Param("ongId") Long ongId, @Param("ahora") LocalDateTime ahora);

    void deleteByOngId(Long ongId);
}
