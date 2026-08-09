package com.ceiba.medisalud.shared.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class OpenApiConfig {

    @Bean
    OpenAPI mediSaludOpenApi() {
        return new OpenAPI().info(new Info()
                .title("MediSalud API")
                .description("Sistema de agendamiento de citas medicas - registro de medicos y pacientes, "
                        + "reserva, disponibilidad, cancelacion, reprogramacion y listado de citas.")
                .version("v1"));
    }
}
