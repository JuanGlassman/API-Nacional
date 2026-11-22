package com.proyecto.nacional.config;

import com.proyecto.nacional.model.SeedVersion;
import com.proyecto.nacional.repository.InventarioOngRepository;
import com.proyecto.nacional.repository.OfertaRepository;
import com.proyecto.nacional.repository.OngCategoriaNivelRepository;
import com.proyecto.nacional.repository.ReservaRepository;
import com.proyecto.nacional.repository.SeedVersionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Permite volver a cargar el estado de la demo (inventarios, niveles de competencia, ofertas y reservas)
 * sin tocar la base a mano. Se activa con la variable de entorno SEED_VERSION: si tiene un valor distinto
 * del último que se aplicó, borra ese estado y {@link DatosIniciales} lo vuelve a cargar. Con el mismo
 * valor no hace nada, así que reiniciar el servicio (Render lo hace al dormirlo) no pisa una demo en curso.
 * Para recargar otra vez hay que cambiar el valor (demo-1, demo-2...). Vacía = desactivado.
 * No borra ONGs, recursos ni cuentas.
 */
@Slf4j
@Component
@Order(1)
public class ReinicioDatosDemo implements CommandLineRunner {

    private final ReservaRepository reservaRepository;
    private final OfertaRepository ofertaRepository;
    private final InventarioOngRepository inventarioOngRepository;
    private final OngCategoriaNivelRepository nivelRepository;
    private final SeedVersionRepository seedVersionRepository;
    private final String seedVersion;

    public ReinicioDatosDemo(ReservaRepository reservaRepository,
                             OfertaRepository ofertaRepository,
                             InventarioOngRepository inventarioOngRepository,
                             OngCategoriaNivelRepository nivelRepository,
                             SeedVersionRepository seedVersionRepository,
                             @Value("${seed.version:}") String seedVersion) {
        this.reservaRepository = reservaRepository;
        this.ofertaRepository = ofertaRepository;
        this.inventarioOngRepository = inventarioOngRepository;
        this.nivelRepository = nivelRepository;
        this.seedVersionRepository = seedVersionRepository;
        this.seedVersion = seedVersion == null ? "" : seedVersion.trim();
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (seedVersion.isEmpty()) {
            return;
        }
        SeedVersion aplicada = seedVersionRepository.findById(SeedVersion.ID).orElse(null);
        if (aplicada != null && seedVersion.equals(aplicada.getVersion())) {
            log.info("Seed: la versión '{}' ya está aplicada, no se reinicia nada", seedVersion);
            return;
        }
        reservaRepository.deleteAllInBatch();
        ofertaRepository.deleteAll();
        inventarioOngRepository.deleteAllInBatch();
        nivelRepository.deleteAllInBatch();
        seedVersionRepository.save(new SeedVersion(seedVersion));
        log.warn("Seed: estado reiniciado para la versión '{}' (reservas, ofertas, inventarios y niveles)",
                seedVersion);
    }
}
