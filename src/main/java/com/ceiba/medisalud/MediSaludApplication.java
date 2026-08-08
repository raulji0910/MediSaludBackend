package com.ceiba.medisalud;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class MediSaludApplication {

    public static void main(String[] args) {
        SpringApplication.run(MediSaludApplication.class, args);
    }

}
