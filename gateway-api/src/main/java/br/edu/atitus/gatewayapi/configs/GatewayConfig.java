package br.edu.atitus.gatewayapi.configs;


import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;

@Configuration
public class GatewayConfig {

    @Bean
    public RouteLocator getRoutes(RouteLocatorBuilder builder){
        return builder.routes()
                // Rotas Públicas
                .route("product-api", route -> route
                        .path("/products/**")
                        .uri("lb://product-api"))

                .route("currency-api", route -> route
                        .path("/currencies/**")
                        .uri("lb://currency-api"))

                .build();
    }
}