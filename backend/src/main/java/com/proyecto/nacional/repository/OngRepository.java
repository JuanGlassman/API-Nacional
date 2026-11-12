package com.proyecto.nacional.repository;

import com.proyecto.nacional.model.Ong;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface OngRepository extends JpaRepository<Ong, Long> {

    List<Ong> findAllByOrderByRazonSocialAsc();

    Optional<Ong> findByCodigo(String codigo);

    List<Ong> findAllByCodigoIn(Collection<String> codigos);

    boolean existsByCodigo(String codigo);

    boolean existsByRazonSocial(String razonSocial);

    boolean existsByBonitaGroupPath(String bonitaGroupPath);

    boolean existsByRazonSocialAndIdNot(String razonSocial, Long id);

    boolean existsByBonitaGroupPathAndIdNot(String bonitaGroupPath, Long id);
}
