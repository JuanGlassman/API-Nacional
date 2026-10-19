package com.proyecto.nacional.config;

import com.proyecto.nacional.model.Cuenta;
import com.proyecto.nacional.model.InventarioOng;
import com.proyecto.nacional.model.Ong;
import com.proyecto.nacional.model.Recurso;
import com.proyecto.nacional.model.Rol;
import com.proyecto.nacional.repository.CuentaRepository;
import com.proyecto.nacional.repository.InventarioOngRepository;
import com.proyecto.nacional.repository.OngRepository;
import com.proyecto.nacional.repository.RecursoRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Carga al arrancar los recursos, las ONGs y sus inventarios, con los mismos datos que el seed de la
 * aplicación local (db/seed/01-reset-y-seed.sql). Cada tabla se carga solo si está vacía, así que
 * reiniciar la API no pisa lo que ya se editó. Si hay contraseña configurada, también crea una
 * cuenta de rol ONG por cada ONG del seed.
 */
@Slf4j
@Component
@Order(2)
public class DatosIniciales implements CommandLineRunner {

    private record RecursoSeed(String nombre, String unidadMedida) {
    }

    private record OngSeed(String razonSocial, String bonitaGroupPath, String username) {
    }

    // (ong, recurso, cantidad); ong y recurso son el número de orden del seed local (1 a N)
    private record InventarioSeed(int ong, int recurso, int cantidad) {
    }

    private static final List<RecursoSeed> RECURSOS = List.of(
            new RecursoSeed("Raciones de alimento", "raciones"),   // 1
            new RecursoSeed("Paramédicos", "personas"),            // 2
            new RecursoSeed("Frazadas", "unidades"),               // 3
            new RecursoSeed("Bombas de agua", "unidades"),         // 4
            new RecursoSeed("Agua potable", "litros"),             // 5
            new RecursoSeed("Carpas", "unidades"),                 // 6
            new RecursoSeed("Botes de rescate", "unidades"),       // 7
            new RecursoSeed("Kits de higiene", "unidades"),        // 8
            new RecursoSeed("Generadores eléctricos", "unidades"), // 9
            new RecursoSeed("Médicos", "personas"));               // 10

    private static final List<OngSeed> ONGS = List.of(
            new OngSeed("Cruz Roja Argentina – Filial La Plata", "/ONG/CruzRojaLaPlata", "cruzroja"),            // 1
            new OngSeed("Cáritas Arquidiócesis de Buenos Aires", "/ONG/CaritasBuenosAires", "caritas"),          // 2
            new OngSeed("Bomberos Voluntarios de La Plata", "/ONG/BomberosLaPlata", "bomberos"),                 // 3
            new OngSeed("Fundación Banco de Alimentos Buenos Aires", "/ONG/BancoAlimentosBA", "bancoalimentos"), // 4
            new OngSeed("Techo Argentina – Regional Buenos Aires", "/ONG/TechoBuenosAires", "techo"));           // 5

    private static final List<InventarioSeed> INVENTARIO = List.of(
            new InventarioSeed(1, 2, 10), new InventarioSeed(1, 3, 200),       // Cruz Roja: paramédicos, frazadas
            new InventarioSeed(2, 1, 800), new InventarioSeed(2, 3, 150),      // Cáritas: raciones, frazadas
            new InventarioSeed(3, 2, 6), new InventarioSeed(3, 4, 4),          // Bomberos: paramédicos, bombas de agua
            new InventarioSeed(4, 1, 1500),                                    // Banco de Alimentos: raciones
            new InventarioSeed(5, 3, 300), new InventarioSeed(5, 4, 2),        // Techo: frazadas, bombas de agua
            new InventarioSeed(1, 8, 500), new InventarioSeed(1, 10, 4),       // Cruz Roja: kits de higiene, médicos
            new InventarioSeed(2, 5, 20000), new InventarioSeed(2, 8, 800),    // Cáritas: agua potable, kits de higiene
            new InventarioSeed(3, 7, 6), new InventarioSeed(3, 9, 5),          // Bomberos: botes de rescate, generadores
            new InventarioSeed(4, 5, 30000),                                   // Banco de Alimentos: agua potable
            new InventarioSeed(5, 6, 80), new InventarioSeed(5, 9, 3));        // Techo: carpas, generadores

    private final RecursoRepository recursoRepository;
    private final OngRepository ongRepository;
    private final InventarioOngRepository inventarioOngRepository;
    private final CuentaRepository cuentaRepository;
    private final PasswordEncoder passwordEncoder;
    private final String passwordOngs;

    public DatosIniciales(RecursoRepository recursoRepository,
                          OngRepository ongRepository,
                          InventarioOngRepository inventarioOngRepository,
                          CuentaRepository cuentaRepository,
                          PasswordEncoder passwordEncoder,
                          @Value("${cuentas.ong.password:}") String passwordOngs) {
        this.recursoRepository = recursoRepository;
        this.ongRepository = ongRepository;
        this.inventarioOngRepository = inventarioOngRepository;
        this.cuentaRepository = cuentaRepository;
        this.passwordEncoder = passwordEncoder;
        this.passwordOngs = passwordOngs;
    }

    @Override
    @Transactional
    public void run(String... args) {
        Map<Integer, Recurso> recursos = cargarRecursos();
        Map<Integer, Ong> ongs = cargarOngs();
        cargarInventario(recursos, ongs);
        crearCuentasOng(ongs);
    }

    private Map<Integer, Recurso> cargarRecursos() {
        boolean vacia = recursoRepository.count() == 0;
        Map<Integer, Recurso> porOrden = new HashMap<>();
        for (int i = 0; i < RECURSOS.size(); i++) {
            RecursoSeed seed = RECURSOS.get(i);
            Recurso recurso = vacia ? null : recursoRepository.findByNombre(seed.nombre()).orElse(null);
            if (vacia) {
                recurso = new Recurso();
                recurso.setNombre(seed.nombre());
                recurso.setUnidadMedida(seed.unidadMedida());
                recurso = recursoRepository.save(recurso);
            }
            if (recurso != null) {
                porOrden.put(i + 1, recurso);
            }
        }
        if (vacia) {
            log.info("Seed: {} recursos creados", RECURSOS.size());
        }
        return porOrden;
    }

    private Map<Integer, Ong> cargarOngs() {
        boolean vacia = ongRepository.count() == 0;
        Map<Integer, Ong> porOrden = new HashMap<>();
        if (!vacia) {
            return porOrden;
        }
        for (int i = 0; i < ONGS.size(); i++) {
            OngSeed seed = ONGS.get(i);
            Ong ong = new Ong();
            ong.setRazonSocial(seed.razonSocial());
            ong.setBonitaGroupPath(seed.bonitaGroupPath());
            porOrden.put(i + 1, ongRepository.save(ong));
        }
        log.info("Seed: {} ONGs creadas", ONGS.size());
        return porOrden;
    }

    // Solo se carga junto con las ONGs: si ya existían, su inventario ya fue editado
    private void cargarInventario(Map<Integer, Recurso> recursos, Map<Integer, Ong> ongs) {
        if (ongs.isEmpty() || recursos.size() != RECURSOS.size()) {
            return;
        }
        for (InventarioSeed seed : INVENTARIO) {
            InventarioOng inventario = new InventarioOng();
            inventario.setOng(ongs.get(seed.ong()));
            inventario.setRecurso(recursos.get(seed.recurso()));
            inventario.setCantidadDisponible(seed.cantidad());
            inventarioOngRepository.save(inventario);
        }
        log.info("Seed: {} ítems de inventario creados", INVENTARIO.size());
    }

    private void crearCuentasOng(Map<Integer, Ong> ongs) {
        if (ongs.isEmpty()) {
            return;
        }
        if (passwordOngs.isBlank()) {
            log.warn("No se crearon las cuentas de las ONGs: falta la variable de entorno CUENTAS_ONG_PASSWORD");
            return;
        }
        for (int i = 0; i < ONGS.size(); i++) {
            String username = ONGS.get(i).username();
            if (cuentaRepository.existsByUsername(username)) {
                continue;
            }
            Cuenta cuenta = new Cuenta();
            cuenta.setUsername(username);
            cuenta.setPasswordHash(passwordEncoder.encode(passwordOngs));
            cuenta.setRol(Rol.ONG);
            cuenta.setHabilitada(true);
            cuenta.setOng(ongs.get(i + 1));
            cuentaRepository.save(cuenta);
            log.info("Cuenta '{}' creada con rol ONG", username);
        }
    }
}
