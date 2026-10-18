package com.proyecto.nacional.model;

// Roles de las cuentas que pueden autenticarse en el Sistema Nacional
public enum Rol {
    ADMIN,
    ONG,
    // Sistemas externos que consumen la API (por ejemplo, Bonita)
    SISTEMA_CLIENTE
}
