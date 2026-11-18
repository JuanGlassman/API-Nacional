package com.proyecto.nacional.model;

/**
 * Nivel con el que el Sistema Nacional habilita a una ONG en una categoría. Cuanto menor la prioridad,
 * antes se le asignan recursos cuando hay competencia. Sin nivel cargado la ONG no está habilitada
 * en esa categoría y no se le reserva nada.
 */
public enum NivelCompetencia {
    ESPECIALISTA(1),
    APOYO(2);

    private final int prioridad;

    NivelCompetencia(int prioridad) {
        this.prioridad = prioridad;
    }

    public int getPrioridad() {
        return prioridad;
    }
}
