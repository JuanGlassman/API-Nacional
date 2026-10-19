package com.proyecto.nacional.controller;

import com.proyecto.nacional.dto.reservas.ReservaLiberacionRequest;
import com.proyecto.nacional.dto.reservas.ReservaRequest;
import com.proyecto.nacional.dto.reservas.ReservaResponse;
import com.proyecto.nacional.dto.reservas.ReservaTemporalRequest;
import com.proyecto.nacional.service.ReservaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "Reservas", description = "Reserva temporal, reserva definitiva y liberación de los recursos de las ONGs")
public class ReservaController {

    private final ReservaService reservaService;

    @PostMapping("/reservas/temporal")
    @PreAuthorize("hasAnyRole('SISTEMA_CLIENTE', 'ADMIN')")
    @Operation(summary = "Reserva temporal (validación)",
            description = "Bloquea los recursos que ofrece una oferta ya guardada por un día. Todo o nada: si algún "
                    + "recurso no alcanza responde 409 y no bloquea nada. Repetirla reemplaza los bloqueos anteriores.")
    @ApiResponse(responseCode = "201", description = "Recursos bloqueados")
    public ResponseEntity<ReservaResponse> reservarTemporal(@Valid @RequestBody ReservaTemporalRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reservaService.reservarTemporal(request));
    }

    @PostMapping("/reservas/definitiva")
    @PreAuthorize("hasAnyRole('SISTEMA_CLIENTE', 'ADMIN')")
    @Operation(summary = "Reserva definitiva (oferta adjudicada)",
            description = "Convierte los bloqueos de la oferta en compromiso, que dura hasta la liberación explícita. "
                    + "Si el bloqueo venció, vuelve a comprobar que los recursos sigan libres.")
    public ReservaResponse reservarDefinitiva(@Valid @RequestBody ReservaRequest request) {
        return reservaService.reservarDefinitiva(request);
    }

    @PostMapping("/reservas/liberacion")
    @PreAuthorize("hasAnyRole('SISTEMA_CLIENTE', 'ADMIN')")
    @Operation(summary = "Liberación (finalización)",
            description = "Libera los recursos reservados por la oferta, o solo los de una ONG si se indica ongId. "
                    + "Se puede repetir sin efecto.")
    public ReservaResponse liberar(@Valid @RequestBody ReservaLiberacionRequest request) {
        return reservaService.liberar(request);
    }

    @GetMapping("/reservas")
    @Operation(summary = "Consultar las reservas de una oferta")
    public ReservaResponse consultar(@RequestParam Long emergenciaId, @RequestParam Long loteId,
                                     @RequestParam Long ofertaId) {
        return reservaService.consultar(emergenciaId, loteId, ofertaId);
    }
}
