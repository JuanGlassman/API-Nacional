package com.proyecto.nacional.repository;

import com.proyecto.nacional.model.Ong;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OngRepository extends JpaRepository<Ong, Long> {

    List<Ong> findAllByOrderByRazonSocialAsc();

    boolean existsByRazonSocial(String razonSocial);

    boolean existsByBonitaGroupPath(String bonitaGroupPath);

    boolean existsByRazonSocialAndIdNot(String razonSocial, Long id);

    boolean existsByBonitaGroupPathAndIdNot(String bonitaGroupPath, Long id);
}
