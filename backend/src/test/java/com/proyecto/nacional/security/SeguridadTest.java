package com.proyecto.nacional.security;

import com.proyecto.nacional.config.SecurityConfig;
import com.proyecto.nacional.controller.AuthController;
import com.proyecto.nacional.exception.GlobalExceptionHandler;
import com.proyecto.nacional.model.Cuenta;
import com.proyecto.nacional.model.Rol;
import com.proyecto.nacional.repository.CuentaRepository;
import com.proyecto.nacional.service.AuthService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Login y filtro JWT de punta a punta sobre MockMvc, sin base de datos (el repositorio está mockeado)
@WebMvcTest(controllers = {AuthController.class, SeguridadTest.ControllerDePrueba.class})
@Import({SecurityConfig.class, JwtService.class, CuentaUserDetailsService.class,
        ManejadorErroresSeguridad.class, AuthService.class, GlobalExceptionHandler.class,
        SeguridadTest.ControllerDePrueba.class})
@TestPropertySource(properties = {
        "jwt.secret=" + SeguridadTest.SECRETO,
        "jwt.expiration-minutes=60"
})
class SeguridadTest {

    static final String SECRETO = "secreto-de-prueba-para-los-tests-de-jwt";
    private static final String PASSWORD = "clave-correcta";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private CuentaRepository cuentaRepository;

    // Endpoints protegidos de ejemplo para probar el filtro y los roles
    @RestController
    static class ControllerDePrueba {

        @GetMapping("/prueba/autenticado")
        String autenticado() {
            return "ok";
        }

        @GetMapping("/prueba/admin")
        @PreAuthorize("hasRole('ADMIN')")
        String soloAdmin() {
            return "ok";
        }
    }

    // ---------- /login ----------

    @Test
    void loginCorrectoDevuelveUnTokenValido() throws Exception {
        registrarCuenta("bonita", Rol.SISTEMA_CLIENTE, true);

        String respuesta = login("bonita", PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipo").value("Bearer"))
                .andExpect(jsonPath("$.expiraEn").value(3600))
                .andExpect(jsonPath("$.rol").value("SISTEMA_CLIENTE"))
                .andReturn().getResponse().getContentAsString();

        String token = respuesta.replaceAll(".*\"token\":\"([^\"]+)\".*", "$1");
        assertThat(jwtService.extraerClaims(token).getSubject()).isEqualTo("bonita");
    }

    @Test
    void loginConPasswordIncorrectaDevuelve401() throws Exception {
        registrarCuenta("bonita", Rol.SISTEMA_CLIENTE, true);

        login("bonita", "otra-clave")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void loginDeCuentaInexistenteDevuelve401() throws Exception {
        when(cuentaRepository.findByUsername(anyString())).thenReturn(Optional.empty());

        login("nadie", PASSWORD)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void loginDeCuentaDeshabilitadaDevuelve401() throws Exception {
        registrarCuenta("ong-baja", Rol.ONG, false);

        login("ong-baja", PASSWORD)
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginSinUsuarioNiPasswordDevuelve400() throws Exception {
        mockMvc.perform(post("/login").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles.length()").value(2));
    }

    @Test
    void loginSinCuerpoDevuelve400() throws Exception {
        mockMvc.perform(post("/login").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    // ---------- Filtro JWT ----------

    @Test
    void sinTokenDevuelve401ConFormatoDeError() throws Exception {
        mockMvc.perform(get("/prueba/autenticado"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    void conTokenValidoAccedeAUnEndpointAutenticado() throws Exception {
        Cuenta cuenta = registrarCuenta("ong-1", Rol.ONG, true);

        conToken(get("/prueba/autenticado"), jwtService.generarToken(cuenta))
                .andExpect(status().isOk());
    }

    @Test
    void tokenAdulteradoDevuelve401() throws Exception {
        Cuenta cuenta = registrarCuenta("ong-1", Rol.ONG, true);
        String token = jwtService.generarToken(cuenta) + "x";

        conToken(get("/prueba/autenticado"), token)
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenVencidoDevuelve401() throws Exception {
        registrarCuenta("ong-1", Rol.ONG, true);
        Instant haceDosHoras = Instant.now().minus(2, ChronoUnit.HOURS);
        String token = Jwts.builder()
                .subject("ong-1")
                .issuedAt(Date.from(haceDosHoras))
                .expiration(Date.from(haceDosHoras.plus(1, ChronoUnit.HOURS)))
                .signWith(Keys.hmacShaKeyFor(SECRETO.getBytes(StandardCharsets.UTF_8)))
                .compact();

        conToken(get("/prueba/autenticado"), token)
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenDeUnaCuentaDeshabilitadaDevuelve401() throws Exception {
        Cuenta cuenta = registrarCuenta("ong-baja", Rol.ONG, false);

        conToken(get("/prueba/autenticado"), jwtService.generarToken(cuenta))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rolSinPermisoDevuelve403() throws Exception {
        Cuenta cuenta = registrarCuenta("bonita", Rol.SISTEMA_CLIENTE, true);

        conToken(get("/prueba/admin"), jwtService.generarToken(cuenta))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void rolConPermisoDevuelve200() throws Exception {
        Cuenta cuenta = registrarCuenta("admin", Rol.ADMIN, true);

        conToken(get("/prueba/admin"), jwtService.generarToken(cuenta))
                .andExpect(status().isOk());
    }

    // ---------- Helpers ----------

    private Cuenta registrarCuenta(String username, Rol rol, boolean habilitada) {
        Cuenta cuenta = new Cuenta();
        cuenta.setUsername(username);
        cuenta.setPasswordHash(passwordEncoder.encode(PASSWORD));
        cuenta.setRol(rol);
        cuenta.setHabilitada(habilitada);
        when(cuentaRepository.findByUsername(username)).thenReturn(Optional.of(cuenta));
        return cuenta;
    }

    private ResultActions login(String username, String password) throws Exception {
        String cuerpo = "{\"username\":\"%s\",\"password\":\"%s\"}".formatted(username, password);
        return mockMvc.perform(post("/login").contentType(MediaType.APPLICATION_JSON).content(cuerpo));
    }

    private ResultActions conToken(MockHttpServletRequestBuilder request, String token) throws Exception {
        return mockMvc.perform(request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token));
    }
}
