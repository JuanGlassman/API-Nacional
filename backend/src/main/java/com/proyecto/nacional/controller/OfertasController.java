package com.proyecto.nacional.controller;

import com.proyecto.nacional.dto.ofertas.OfertasConsolidadasRequest;
import com.proyecto.nacional.dto.ofertas.OfertasConsolidadasResponse;
import com.proyecto.nacional.service.OfertasService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Ofertas", description = "Obtención de las ofertas de ayuda disponibles para las emergencias")
public class OfertasController {

    private final OfertasService ofertasService;


    @PostMapping("/ofertas")
    @PreAuthorize("hasRole('SISTEMA_CLIENTE')")
    @Operation(summary = "Recibir ofertas consolidadas desde Bonita")
    @ApiResponse(responseCode = "201", description = "Ofertas almacenadas correctamente")
    public ResponseEntity<OfertasConsolidadasResponse> guardar(
            @Valid @RequestBody OfertasConsolidadasRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ofertasService.guardar(request));
    }

    @GetMapping("/ofertas")
    @Operation(summary = "Listar ofertas consolidadas recibidas")
    public OfertasConsolidadasResponse listar(
            @RequestParam Long emergenciaId,
            @RequestParam Long loteId) {
        return ofertasService.listar(emergenciaId, loteId);
    }

}
