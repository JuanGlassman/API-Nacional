package com.proyecto.nacional.controller;

import com.proyecto.nacional.dto.ongs.InventarioOngResponse;
import com.proyecto.nacional.dto.ongs.InventarioRequest;
import com.proyecto.nacional.dto.ongs.OngRequest;
import com.proyecto.nacional.dto.ongs.OngResponse;
import com.proyecto.nacional.dto.ongs.RecursoResponse;
import com.proyecto.nacional.service.OngService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;

@RestController
@RequiredArgsConstructor
@Tag(name = "ONGs", description = "Alta, baja y modificación de ONGs y de su inventario de recursos")
public class OngController {

    private final OngService ongService;

    @GetMapping("/ongs")
    @Operation(summary = "Listar las ONGs")
    public List<OngResponse> listar() {
        return ongService.listar();
    }

    @GetMapping("/ongs/{id}")
    @Operation(summary = "Obtener una ONG")
    public OngResponse obtener(@PathVariable Long id) {
        return ongService.obtener(id);
    }

    @PostMapping("/ongs")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Dar de alta una ONG (solo administrador)")
    @ApiResponse(responseCode = "201", description = "ONG creada")
    public ResponseEntity<OngResponse> crear(@Valid @RequestBody OngRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ongService.crear(request));
    }

    @PutMapping("/ongs/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ONG')")
    @Operation(summary = "Modificar una ONG (administrador, o la propia ONG)")
    public OngResponse actualizar(@PathVariable Long id, @Valid @RequestBody OngRequest request,
                                  Authentication authentication) {
        return ongService.actualizar(id, request, authentication);
    }

    @DeleteMapping("/ongs/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Dar de baja una ONG y su inventario (solo administrador)")
    @ApiResponse(responseCode = "204", description = "ONG eliminada")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        ongService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/recursos")
    @Operation(summary = "Listar el catálogo de recursos")
    public List<RecursoResponse> listarRecursos() {
        return ongService.listarRecursos();
    }

    @GetMapping("/ongs/inventario")
    @Operation(summary = "Inventario de varias ONGs")
    public List<InventarioOngResponse> inventarioPorOng(@RequestParam("ongIds") Set<Long> ongIds) {
        return ongService.inventarioPorOng(ongIds);
    }

    @GetMapping("/ongs/{id}/inventario")
    @Operation(summary = "Inventario de una ONG")
    public InventarioOngResponse inventario(@PathVariable Long id) {
        return ongService.inventario(id);
    }

    @PutMapping("/ongs/{id}/inventario")
    @PreAuthorize("hasAnyRole('ADMIN', 'ONG')")
    @Operation(summary = "Alta o modificación de ítems del inventario (administrador, o la propia ONG)")
    public InventarioOngResponse guardarInventario(@PathVariable Long id,
                                                   @Valid @RequestBody InventarioRequest request,
                                                   Authentication authentication) {
        return ongService.guardarInventario(id, request, authentication);
    }

    @DeleteMapping("/ongs/{id}/inventario/{recursoId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ONG')")
    @Operation(summary = "Baja de un ítem del inventario (administrador, o la propia ONG)")
    @ApiResponse(responseCode = "204", description = "Ítem eliminado")
    public ResponseEntity<Void> eliminarItemInventario(@PathVariable Long id, @PathVariable Long recursoId,
                                                       Authentication authentication) {
        ongService.eliminarItemInventario(id, recursoId, authentication);
        return ResponseEntity.noContent().build();
    }
}
