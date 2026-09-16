package com.kfokam48.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration OpenAPI / Swagger de l'API d'inventaire.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI inventoryOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Inventory API")
                        .description("API de Gestion d'un Inventaire de Produits avec Alertes et Mouvements de Stock")
                        .version("1.0.0"));
    }
}
