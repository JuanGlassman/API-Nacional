package com.proyecto.nacional.controller;

import com.proyecto.nacional.config.SecurityConfig;
import com.proyecto.nacional.dto.ongs.OngResponse;
import com.proyecto.nacional.exception.GlobalExceptionHandler;
import com.proyecto.nacional.model.Cuenta;
import com.proyecto.nacional.model.Rol;
import com.proyecto.nacional.repository.CuentaRepository;
import com.proyecto.nacional.security.CuentaUserDetailsService;
import com.proyecto.nacional.security.JwtService;
import com.proyecto.nacional.security.ManejadorErroresSeguridad;
import com.proyecto.nacional.service.OngService;
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

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Permisos de los endpoints de ONGs sobre MockMvc; el service está mockeado
@WebMvcTest(controllers = OngController.class)
@Import({SecurityConfig.class, JwtService.class, CuentaUserDetailsService.class,
        ManejadorErroresSeguridad.class, GlobalExceptionHandler.class})
@TestPropertySource(properties = {
        "jwt.secret=secreto-de-prueba-para-los-tests-de-jwt",
        "jwt.expiration-minutes=60"
})
class OngControllerTest {

    private static final String BODY_ONG = "{\"razonSocial\":\"Nueva ONG\",\"bonitaGroupPath\":\"/ONG/Nueva\"}";
    private static final String BODY_INVENTARIO = "{\"recursos\":[{\"recursoId\":1,\"cantidadDisponible\":5}]}";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private OngService ongService;

    @MockitoBean
    private CuentaRepository cuentaRepository;

    @Test
    void sinTokenDevuelve401() throws Exception {
        mockMvc.perform(get("/ongs")).andExpect(status().isUnauthorized());
    }

    @Test
    void cualquierCuentaAutenticadaListaLasOngs() throws Exception {
        when(ongService.listar()).thenReturn(java.util.List.of(new OngResponse(1L, "Cruz Roja", "/ONG/CruzRoja")));

        conToken(get("/ongs"), Rol.SISTEMA_CLIENTE)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].razonSocial").value("Cruz Roja"))
                .andExpect(jsonPath("$[0].bonitaGroupPath").value("/ONG/CruzRoja"));
    }

    @Test
    void adminDaDeAltaUnaOng() throws Exception {
        when(ongService.crear(any())).thenReturn(new OngResponse(9L, "Nueva ONG", "/ONG/Nueva"));

        conToken(post("/ongs").contentType(MediaType.APPLICATION_JSON).content(BODY_ONG), Rol.ADMIN)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(9));
    }

    @Test
    void altaConDatosInvalidosDevuelve400() throws Exception {
        conToken(post("/ongs").contentType(MediaType.APPLICATION_JSON).content("{}"), Rol.ADMIN)
                .andExpect(status().isBadRequest());
        verifyNoInteractions(ongService);
    }

    @Test
    void ongNoPuedeDarDeAltaNiDeBajaOngs() throws Exception {
        conToken(post("/ongs").contentType(MediaType.APPLICATION_JSON).content(BODY_ONG), Rol.ONG)
                .andExpect(status().isForbidden());
        conToken(delete("/ongs/1"), Rol.ONG).andExpect(status().isForbidden());
        verifyNoInteractions(ongService);
    }

    @Test
    void bonitaNoPuedeEscribirOngNiInventario() throws Exception {
        conToken(post("/ongs").contentType(MediaType.APPLICATION_JSON).content(BODY_ONG), Rol.SISTEMA_CLIENTE)
                .andExpect(status().isForbidden());
        conToken(put("/ongs/1").contentType(MediaType.APPLICATION_JSON).content(BODY_ONG), Rol.SISTEMA_CLIENTE)
                .andExpect(status().isForbidden());
        conToken(put("/ongs/1/inventario").contentType(MediaType.APPLICATION_JSON).content(BODY_INVENTARIO),
                Rol.SISTEMA_CLIENTE).andExpect(status().isForbidden());
        conToken(delete("/ongs/1/inventario/1"), Rol.SISTEMA_CLIENTE).andExpect(status().isForbidden());
        verifyNoInteractions(ongService);
    }

    @Test
    void ongEditaSuInventarioDelegandoElControlDePertenenciaAlService() throws Exception {
        conToken(put("/ongs/1/inventario").contentType(MediaType.APPLICATION_JSON).content(BODY_INVENTARIO),
                Rol.ONG).andExpect(status().isOk());
        verify(ongService).guardarInventario(eq(1L), any(), any());
    }

    @Test
    void adminDaDeBajaUnaOng() throws Exception {
        conToken(delete("/ongs/1"), Rol.ADMIN).andExpect(status().isNoContent());
        verify(ongService).eliminar(1L);
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
