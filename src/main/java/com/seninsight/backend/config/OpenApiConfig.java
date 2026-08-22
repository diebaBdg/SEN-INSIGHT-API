package com.seninsight.backend.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class OpenApiConfig {

    @Value("${server.servlet.context-path:/}")
    private String contextPath;

    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    @Value("${app.backend.url:}")
    private String backendUrl;

    @Bean
    public OpenAPI customOpenAPI() {
        List<Server> servers = new ArrayList<>();

        if ("preprod".equals(activeProfile) || "prod".equals(activeProfile)) {
            if (!backendUrl.isBlank()) {
                Server httpsServer = new Server();
                httpsServer.setUrl(backendUrl + contextPath);
                httpsServer.setDescription("Production HTTPS Server");
                servers.add(httpsServer);
            }
        } else {
            Server localhostServer = new Server();
            localhostServer.setUrl("http://localhost:9292" + contextPath);
            localhostServer.setDescription("Local Development Server");
            servers.add(localhostServer);
        }

        SecurityScheme bearerScheme = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("Entrez le token JWT obtenu via /auth/login");

        return new OpenAPI()
                .info(new Info()
                        .title("Senegal Licence Platform API")
                        .version("1.0.0")
                        .description("API de gestion des agréments au Sénégal"))
                .servers(servers)
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", bearerScheme))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }
}