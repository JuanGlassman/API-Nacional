package com.proyecto.nacional.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Misma forma que la ONG de la aplicación local
@Entity
@Table(name = "ong")
@Getter
@Setter
@NoArgsConstructor
public class Ong {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "razon_social", nullable = false, unique = true, length = 200)
    private String razonSocial;

    // Path del subgrupo de la ONG en la organización de Bonita (ej. /ONG/CruzRojaLaPlata)
    @Column(name = "bonita_group_path", nullable = false, unique = true, length = 200)
    private String bonitaGroupPath;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Ong)) {
            return false;
        }
        Ong other = (Ong) o;
        return id != null && id.equals(other.getId());
    }

    @Override
    public int hashCode() {
        return Ong.class.hashCode();
    }
}
