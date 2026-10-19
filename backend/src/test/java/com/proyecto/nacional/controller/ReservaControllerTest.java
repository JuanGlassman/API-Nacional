package com.proyecto.nacional.controller;

import com.proyecto.nacional.config.SecurityConfig;
import com.proyecto.nacional.dto.reservas.ReservaResponse;
import com.proyecto.nacional.exception.ConflictoException;
import com.proyecto.nacional.exception.GlobalExceptionHandler;
import com.proyecto.nacional.exception.SolicitudInvalidaException;
import com.proyecto.nacional.model.Cuenta;
import com.proyecto.nacional.model.Rol;
import com.proyecto.nacional.repository.CuentaRepository;
import com.proyecto.nacional.security.CuentaUserDetailsService;
import com.proyecto.nacional.security.JwtService;
import com.proyecto.nacional.security.ManejadorErroresSeguridad;
import com.proyecto.nacional.service.ReservaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Permisos y errores de los endpoints de reservas sobre MockMvc; el service está mockeado
@WebMvcTest(controllers = ReservaController.class)
@Import({SecurityConfig.class, JwtService.class, CuentaUserDetailsService.class,
        ManejadorErroresSeguridad.class, GlobalExceptionHandler.class})
@TestPropertySource(properties = {
        "jwt.secret=secreto-de-prueba-para-los-tests-de-jwt",
        "jwt.expiration-minutes=60"
})
class ReservaControllerTest {

    private static final String BODY = "{\"emergenciaId\":1,\"loteId\":1,\"ofertaId\":1}";
    private static final ReservaResponse RESPUESTA =
            new ReservaResponse(1L, 1L, 1L, "BLOQUEADA", null, List.of());

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private ReservaService reservaService;

    @MockitoBean
    private CuentaRepository cuentaRepository;

    @Test
    void sinTokenDevuelve401() throws Exception {
        mockMvc.perform(post("/reservas/temporal").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void bonitaReservaTemporalYRecibe201() throws Exception {
        when(reservaService.reservarTemporal(any())).thenReturn(RESPUESTA);

        conToken(post("/reservas/temporal").contentType(MediaType.APPLICATION_JSON).content(BODY),
                Rol.SISTEMA_CLIENTE)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("BLOQUEADA"));
    }

    @Test
    void bonitaHaceLaReservaDefinitivaYLaLiberacion() throws Exception {
        when(reservaService.reservarDefinitiva(any())).thenReturn(RESPUESTA);
        when(reservaService.liberar(any())).thenReturn(RESPUESTA);

        conToken(post("/reservas/definitiva").contentType(MediaType.APPLICATION_JSON).content(BODY),
                Rol.SISTEMA_CLIENTE).andExpect(status().isOk());
        conToken(post("/reservas/liberacion").contentType(MediaType.APPLICATION_JSON).content(BODY),
                Rol.SISTEMA_CLIENTE).andExpect(status().isOk());
    }

    @Test
    void unaOngNoPuedeReservarNiLiberar() throws Exception {
        for (String ruta : List.of("/reservas/temporal", "/reservas/definitiva", "/reservas/liberacion")) {
            conToken(post(ruta).contentType(MediaType.APPLICATION_JSON).content(BODY), Rol.ONG)
                    .andExpect(status().isForbidden());
        }
        verifyNoInteractions(reservaService);
    }

    @Test
    void cuerpoSinCamposObligatoriosDevuelve400() throws Exception {
        conToken(post("/reservas/temporal").contentType(MediaType.APPLICATION_JSON).content("{}"),
                Rol.SISTEMA_CLIENTE).andExpect(status().isBadRequest());
        verifyNoInteractions(reservaService);
    }

    @Test
    void itemConCantidadCeroDevuelve400() throws Exception {
        String body = "{\"emergenciaId\":1,\"loteId\":1,\"ofertaId\":1,"
                + "\"items\":[{\"ongId\":1,\"recursoId\":1,\"cantidad\":0}]}";

        conToken(post("/reservas/temporal").contentType(MediaType.APPLICATION_JSON).content(body),
                Rol.SISTEMA_CLIENTE).andExpect(status().isBadRequest());
    }

    @Test
    void recursosInsuficientesDevuelve409() throws Exception {
        when(reservaService.reservarTemporal(any())).thenThrow(new ConflictoException("Recursos insuficientes"));

        conToken(post("/reservas/temporal").contentType(MediaType.APPLICATION_JSON).content(BODY),
                Rol.SISTEMA_CLIENTE)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("Recursos insuficientes"));
    }

    @Test
    void solicitudInvalidaDelNegocioDevuelve400() throws Exception {
        when(reservaService.reservarTemporal(any())).thenThrow(new SolicitudInvalidaException("Excede lo ofrecido"));

        conToken(post("/reservas/temporal").contentType(MediaType.APPLICATION_JSON).content(BODY),
                Rol.SISTEMA_CLIENTE).andExpect(status().isBadRequest());
    }

    @Test
    void cualquierCuentaAutenticadaConsultaLasReservas() throws Exception {
        when(reservaService.consultar(1L, 1L, 1L)).thenReturn(RESPUESTA);

        conToken(get("/reservas?emergenciaId=1&loteId=1&ofertaId=1"), Rol.ONG)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ofertaId").value(1));
    }

    private ResultActions conToken(MockHttpServletRequestBuilder request, Rol rol) throws Exception {
        Cuenta cuenta = new Cuenta();
        cuenta.setUsername("usuario-" + rol);
        cuenta.setPasswordHash("x");
        cuenta.setRol(rol);
        when(cuentaRepository.findByUsername(cuenta.getUsername())).thenReturn(Optional.of(cuenta));
        return mockMvc.perform(request.header(HttpHeaders.AUTHORIZATION,
                "Bearer " + jwtService.generarToken(cuenta)));
    }
}
