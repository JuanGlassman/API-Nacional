package com.proyecto.nacional.repository;

import com.proyecto.nacional.model.Recurso;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RecursoRepository extends JpaRepository<Recurso, Long> {

    List<Recurso> findAllByOrderByIdAsc();

    Optional<Recurso> findByNombre(String nombre);
}
