package com.proyecto.nacional.mapper;

import com.proyecto.nacional.dto.ongs.DisponibilidadRecursoResponse;
import com.proyecto.nacional.dto.ongs.InventarioOngResponse;
import com.proyecto.nacional.dto.ongs.OngResponse;
import com.proyecto.nacional.dto.ongs.RecursoResponse;
import com.proyecto.nacional.model.InventarioOng;
import com.proyecto.nacional.model.Ong;
import com.proyecto.nacional.model.Recurso;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OngMapper {

    public OngResponse toResponse(Ong ong) {
        return new OngResponse(ong.getId(), ong.getRazonSocial(), ong.getBonitaGroupPath());
    }

    public RecursoResponse toResponse(Recurso recurso) {
        return new RecursoResponse(recurso.getId(), recurso.getNombre(), recurso.getUnidadMedida());
    }

    public InventarioOngResponse toInventarioResponse(Ong ong, List<InventarioOng> inventario) {
        List<DisponibilidadRecursoResponse> recursos = inventario.stream()
                .map(item -> new DisponibilidadRecursoResponse(
                        item.getRecurso().getId(),
                        item.getRecurso().getNombre(),
                        item.getRecurso().getUnidadMedida(),
                        item.getCantidadDisponible()))
                .toList();
        return new InventarioOngResponse(ong.getId(), ong.getRazonSocial(), recursos);
    }
}
