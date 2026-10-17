package com.proyecto.nacional.service;

import com.proyecto.nacional.dto.ofertas.OfertasConsolidadasRequest;
import com.proyecto.nacional.dto.ofertas.OfertasConsolidadasResponse;
import com.proyecto.nacional.model.Oferta;
import com.proyecto.nacional.model.OfertaAporte;
import com.proyecto.nacional.model.OfertaAporteOng;
import com.proyecto.nacional.model.OfertaOng;
import com.proyecto.nacional.repository.OfertaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OfertasService {

    private final OfertaRepository ofertaRepository;

    @Transactional
    public OfertasConsolidadasResponse guardar(OfertasConsolidadasRequest request) {
        ofertaRepository.deleteAll(ofertaRepository.findByEmergenciaIdAndLoteId(
                request.emergenciaId(), request.loteId()));

        List<Oferta> ofertas = request.ofertas().stream()
                .map(oferta -> convertir(oferta, request.emergenciaId(), request.loteId()))
                .toList();
        ofertaRepository.saveAll(ofertas);
        return convertir(request.emergenciaId(), request.loteId(), ofertas);
    }

    @Transactional(readOnly = true)
    public OfertasConsolidadasResponse listar(Long emergenciaId, Long loteId) {
        List<Oferta> ofertas = ofertaRepository.findByEmergenciaIdAndLoteId(emergenciaId, loteId);
        return convertir(emergenciaId, loteId, ofertas);
    }

    private Oferta convertir(OfertasConsolidadasRequest.OfertaRequest source,
                             Long emergenciaId, Long loteId) {
        Oferta target = new Oferta();
        target.setOfertaId(source.id());
        target.setEmergenciaId(emergenciaId);
        target.setLoteId(loteId);
        target.setEstado(source.estado());
        target.setFechaOferta(source.fechaOferta());
        target.setFechaModificacion(source.fechaModificacion());
        target.setNumeroVersion(source.numeroVersion());

        if (source.ongs() != null) {
            source.ongs().forEach(ong -> {
                OfertaOng targetOng = new OfertaOng();
                targetOng.setOngId(ong.id());
                targetOng.setRazonSocial(ong.razonSocial());
                targetOng.setOferta(target);
                target.getOngs().add(targetOng);
            });
        }
        source.aportes().forEach(aporte -> {
            OfertaAporte targetAporte = new OfertaAporte();
            targetAporte.setItemLoteId(aporte.itemLoteId());
            targetAporte.setRecursoNombre(aporte.recursoNombre());
            targetAporte.setCantidadRequerida(aporte.cantidadRequerida());
            targetAporte.setTotalOfrecido(aporte.totalOfrecido());
            targetAporte.setOferta(target);
            if (aporte.porOng() != null) {
                aporte.porOng().forEach(porOng -> {
                    OfertaAporteOng targetPorOng = new OfertaAporteOng();
                    targetPorOng.setOngId(porOng.ongId());
                    targetPorOng.setRazonSocial(porOng.razonSocial());
                    targetPorOng.setCantidadOfrecida(porOng.cantidadOfrecida());
                    targetPorOng.setAporte(targetAporte);
                    targetAporte.getPorOng().add(targetPorOng);
                });
            }
            target.getAportes().add(targetAporte);
        });
        return target;
    }

    private OfertasConsolidadasResponse convertir(Long emergenciaId, Long loteId, List<Oferta> ofertas) {
        return new OfertasConsolidadasResponse(emergenciaId, loteId, ofertas.stream()
                .map(oferta -> new OfertasConsolidadasResponse.OfertaResponse(
                        oferta.getOfertaId(), oferta.getEstado(), oferta.getFechaOferta(),
                        oferta.getFechaModificacion(), oferta.getNumeroVersion(),
                        oferta.getOngs().stream()
                                .map(ong -> new OfertasConsolidadasResponse.OngResponse(
                                        ong.getOngId(), ong.getRazonSocial()))
                                .toList(),
                        oferta.getAportes().stream()
                                .map(aporte -> new OfertasConsolidadasResponse.AporteResponse(
                                        aporte.getItemLoteId(), aporte.getRecursoNombre(),
                                        aporte.getCantidadRequerida(), aporte.getTotalOfrecido(),
                                        aporte.getPorOng().stream()
                                                .map(ong -> new OfertasConsolidadasResponse.AporteOngResponse(
                                                        ong.getOngId(), ong.getRazonSocial(),
                                                        ong.getCantidadOfrecida()))
                                                .toList()))
                                .toList()))
                .toList());
    }
}
