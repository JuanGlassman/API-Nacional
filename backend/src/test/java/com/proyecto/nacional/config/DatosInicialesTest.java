package com.proyecto.nacional.config;

import com.proyecto.nacional.model.CategoriaRecurso;
import com.proyecto.nacional.model.Cuenta;
import com.proyecto.nacional.model.InventarioOng;
import com.proyecto.nacional.model.NivelCompetencia;
import com.proyecto.nacional.model.Ong;
import com.proyecto.nacional.model.OngCategoriaNivel;
import com.proyecto.nacional.model.Recurso;
import com.proyecto.nacional.model.Rol;
import com.proyecto.nacional.repository.CuentaRepository;
import com.proyecto.nacional.repository.InventarioOngRepository;
import com.proyecto.nacional.repository.OngCategoriaNivelRepository;
import com.proyecto.nacional.repository.OngRepository;
import com.proyecto.nacional.repository.RecursoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// El seed debe coincidir con db/seed/01-reset-y-seed.sql de la aplicación local
class DatosInicialesTest {

    private RecursoRepository recursoRepository;
    private OngRepository ongRepository;
    private InventarioOngRepository inventarioOngRepository;
    private OngCategoriaNivelRepository nivelRepository;
    private CuentaRepository cuentaRepository;
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        recursoRepository = mock(RecursoRepository.class);
        ongRepository = mock(OngRepository.class);
        inventarioOngRepository = mock(InventarioOngRepository.class);
        nivelRepository = mock(OngCategoriaNivelRepository.class);
        cuentaRepository = mock(CuentaRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        when(passwordEncoder.encode(any())).thenReturn("hash");
        when(recursoRepository.save(any(Recurso.class))).thenAnswer(i -> i.getArgument(0));
        when(ongRepository.save(any(Ong.class))).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void conTablasVaciasCargaRecursosOngsEInventarioDelSeedLocal() throws Exception {
        when(recursoRepository.count()).thenReturn(0L);
        when(ongRepository.count()).thenReturn(0L);

        seed("").run();

        ArgumentCaptor<Recurso> recursos = ArgumentCaptor.forClass(Recurso.class);
        verify(recursoRepository, atLeast(1)).save(recursos.capture());
        assertThat(recursos.getAllValues()).hasSize(10);
        assertThat(recursos.getAllValues().get(4).getNombre()).isEqualTo("Agua potable");
        assertThat(recursos.getAllValues().get(4).getUnidadMedida()).isEqualTo("litros");

        ArgumentCaptor<Ong> ongs = ArgumentCaptor.forClass(Ong.class);
        verify(ongRepository, atLeast(1)).save(ongs.capture());
        assertThat(ongs.getAllValues()).extracting(Ong::getBonitaGroupPath).containsExactly(
                "/ONG/CruzRojaLaPlata", "/ONG/CaritasBuenosAires", "/ONG/BomberosLaPlata",
                "/ONG/BancoAlimentosBA", "/ONG/TechoBuenosAires");

        ArgumentCaptor<InventarioOng> inventario = ArgumentCaptor.forClass(InventarioOng.class);
        verify(inventarioOngRepository, atLeast(1)).save(inventario.capture());
        List<InventarioOng> items = inventario.getAllValues();
        assertThat(items).hasSize(19);
        assertThat(items.stream().mapToInt(InventarioOng::getCantidadDisponible).sum()).isEqualTo(54_814);
        // Cruz Roja (la primera ONG): paramédicos 20, frazadas 200, kits de higiene 500, médicos 8
        assertThat(items.stream()
                .filter(i -> i.getOng().getRazonSocial().startsWith("Cruz Roja"))
                .collect(Collectors.toMap(i -> i.getRecurso().getNombre(), InventarioOng::getCantidadDisponible)))
                .containsOnly(
                        java.util.Map.entry("Paramédicos", 20), java.util.Map.entry("Frazadas", 200),
                        java.util.Map.entry("Kits de higiene", 500), java.util.Map.entry("Médicos", 8));
        // Techo tiene kits de higiene (SALUD) pero ningún nivel en SALUD: es el caso "sin recursos" de la demo
        assertThat(items.stream()
                .filter(i -> i.getOng().getRazonSocial().startsWith("Techo"))
                .anyMatch(i -> i.getRecurso().getNombre().equals("Kits de higiene"))).isTrue();
        verify(cuentaRepository, never()).save(any());

        // Cada recurso trae su categoría
        assertThat(recursos.getAllValues()).extracting(Recurso::getCategoria).containsExactly(
                CategoriaRecurso.ALIMENTOS_Y_AGUA, CategoriaRecurso.SALUD, CategoriaRecurso.ABRIGO,
                CategoriaRecurso.RESCATE_Y_EQUIPAMIENTO, CategoriaRecurso.ALIMENTOS_Y_AGUA, CategoriaRecurso.ABRIGO,
                CategoriaRecurso.RESCATE_Y_EQUIPAMIENTO, CategoriaRecurso.SALUD,
                CategoriaRecurso.RESCATE_Y_EQUIPAMIENTO, CategoriaRecurso.SALUD);

        // Cada ONG es especialista en la categoría de lo que tiene en su inventario principal
        ArgumentCaptor<OngCategoriaNivel> niveles = ArgumentCaptor.forClass(OngCategoriaNivel.class);
        verify(nivelRepository, atLeast(1)).save(niveles.capture());
        assertThat(niveles.getAllValues()).hasSize(10);
        assertThat(niveles.getAllValues()).filteredOn(n -> n.getNivel() == NivelCompetencia.ESPECIALISTA)
                .extracting(n -> n.getOng().getBonitaGroupPath() + ":" + n.getCategoria())
                .containsExactlyInAnyOrder(
                        "/ONG/CruzRojaLaPlata:SALUD", "/ONG/CaritasBuenosAires:ALIMENTOS_Y_AGUA",
                        "/ONG/BomberosLaPlata:RESCATE_Y_EQUIPAMIENTO", "/ONG/BancoAlimentosBA:ALIMENTOS_Y_AGUA",
                        "/ONG/TechoBuenosAires:ABRIGO");
    }

    @Test
    void conONGsYaCargadasPeroSinNivelesLosCargaPorElPathDelGrupo() throws Exception {
        when(recursoRepository.count()).thenReturn(10L);
        when(ongRepository.count()).thenReturn(5L);
        when(recursoRepository.findByNombre(any())).thenReturn(java.util.Optional.of(new Recurso()));
        when(nivelRepository.count()).thenReturn(0L);
        when(inventarioOngRepository.count()).thenReturn(19L);
        Ong cruzRoja = new Ong();
        cruzRoja.setId(1L);
        cruzRoja.setCodigo("57f6bace-9f0e-4c1b-9ac8-0e3b09069e29");
        cruzRoja.setBonitaGroupPath("/ONG/CruzRojaLaPlata");
        when(ongRepository.findAll()).thenReturn(List.of(cruzRoja));

        seed("").run();

        ArgumentCaptor<OngCategoriaNivel> niveles = ArgumentCaptor.forClass(OngCategoriaNivel.class);
        verify(nivelRepository, atLeast(1)).save(niveles.capture());
        assertThat(niveles.getAllValues()).extracting(OngCategoriaNivel::getCategoria)
                .containsExactlyInAnyOrder(CategoriaRecurso.SALUD, CategoriaRecurso.ABRIGO);
        verify(ongRepository, never()).save(any());
    }

    @Test
    void conTablasCargadasNoPisaNada() throws Exception {
        when(recursoRepository.count()).thenReturn(10L);
        when(ongRepository.count()).thenReturn(5L);
        when(nivelRepository.count()).thenReturn(10L);
        when(inventarioOngRepository.count()).thenReturn(19L);
        Recurso conCategoria = new Recurso();
        conCategoria.setCategoria(CategoriaRecurso.SALUD);
        when(recursoRepository.findByNombre(any())).thenReturn(java.util.Optional.of(conCategoria));

        seed("").run();

        verify(ongRepository, never()).save(any());
        verify(inventarioOngRepository, never()).save(any());
        verify(recursoRepository, never()).save(any());
        verify(nivelRepository, never()).save(any());
    }

    @Test
    void conPasswordCreaUnaCuentaOngPorCadaOng() throws Exception {
        when(recursoRepository.count()).thenReturn(0L);
        when(ongRepository.count()).thenReturn(0L);
        when(cuentaRepository.existsByUsername(any())).thenReturn(false);

        seed("clave").run();

        ArgumentCaptor<Cuenta> cuentas = ArgumentCaptor.forClass(Cuenta.class);
        verify(cuentaRepository, atLeast(1)).save(cuentas.capture());
        assertThat(cuentas.getAllValues()).hasSize(5);
        assertThat(cuentas.getAllValues()).allSatisfy(c -> {
            assertThat(c.getRol()).isEqualTo(Rol.ONG);
            assertThat(c.getOng()).isNotNull();
        });
    }

    private Runner seed(String password) {
        DatosIniciales datos = new DatosIniciales(recursoRepository, ongRepository, inventarioOngRepository,
                nivelRepository, cuentaRepository, passwordEncoder, password);
        return datos::run;
    }

    private interface Runner {
        void run(String... args) throws Exception;
    }
}
