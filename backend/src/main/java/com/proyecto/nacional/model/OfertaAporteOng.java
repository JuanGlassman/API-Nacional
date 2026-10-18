package com.proyecto.nacional.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "oferta_aporte_ong")
@Getter
@Setter
@NoArgsConstructor
public class OfertaAporteOng {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long ongId;
    private String razonSocial;
    private Integer cantidadOfrecida;
    @ManyToOne
    private OfertaAporte aporte;
}
