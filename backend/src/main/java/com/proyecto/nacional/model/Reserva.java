package com.proyecto.nacional.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

// Cantidad de un recurso de una ONG tomada por una oferta. No toca el stock de InventarioOng:
// lo disponible es el stock menos las reservas activas.
@Entity
@Table(
        name = "reserva",
        indexes = {
                @Index(name = "ix_reserva_oferta", columnList = "emergencia_id, lote_id, oferta_id"),
                @Index(name = "ix_reserva_ong_recurso", columnList = "ong_id, recurso_id, estado")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class Reserva {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ong_id", nullable = false)
    private Ong ong;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recurso_id", nullable = false)
    private Recurso recurso;

    @Column(nullable = false)
    private Integer cantidad;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoReserva estado;

    @Column(name = "emergencia_id", nullable = false)
    private Long emergenciaId;

    @Column(name = "lote_id", nullable = false)
    private Long loteId;

    // Id de la oferta que manda Bonita (Oferta.ofertaId)
    @Column(name = "oferta_id", nullable = false)
    private Long ofertaId;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    // Solo mientras está BLOQUEADA
    @Column(name = "expira_en")
    private LocalDateTime expiraEn;

    @Column(name = "fecha_compromiso")
    private LocalDateTime fechaCompromiso;

    @Column(name = "fecha_liberacion")
    private LocalDateTime fechaLiberacion;
}
