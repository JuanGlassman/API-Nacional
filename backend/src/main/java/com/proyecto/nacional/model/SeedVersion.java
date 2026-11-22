package com.proyecto.nacional.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Una sola fila (id = 1): recuerda con qué SEED_VERSION se cargaron por última vez los datos de la demo
@Entity
@Table(name = "seed_version")
@Getter
@Setter
@NoArgsConstructor
public class SeedVersion {

    public static final Long ID = 1L;

    @Id
    private Long id = ID;

    @Column(nullable = false, length = 100)
    private String version;

    public SeedVersion(String version) {
        this.version = version;
    }
}
