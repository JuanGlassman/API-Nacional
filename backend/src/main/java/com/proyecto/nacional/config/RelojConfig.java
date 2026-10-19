package com.proyecto.nacional.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

// El reloj se inyecta para poder fijarlo en los tests de vencimiento de las reservas
@Configuration
public class RelojConfig {

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
