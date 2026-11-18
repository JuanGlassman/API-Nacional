package com.proyecto.nacional.repository;

import com.proyecto.nacional.model.OngCategoriaNivel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface OngCategoriaNivelRepository extends JpaRepository<OngCategoriaNivel, Long> {

    @Query("select n from OngCategoriaNivel n join fetch n.ong where n.ong.codigo in :codigos")
    List<OngCategoriaNivel> findByOngCodigoIn(@Param("codigos") Collection<String> codigos);

    void deleteByOngId(Long ongId);
}
