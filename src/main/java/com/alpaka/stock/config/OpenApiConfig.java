package com.alpaka.stock.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    OpenAPI stockOpenApi() {
        return new OpenAPI()
            .info(new Info()
                .title("Stock Research Backend API")
                .version("0.1.0")
                .description("국내장과 미국장 리서치 자료를 저장하고 프런트 화면 계약으로 제공하는 Spring Boot API")
                .contact(new Contact().name("Stock Desk")))
            .components(new Components()
                .addSecuritySchemes(
                    "access-cookie",
                    new SecurityScheme()
                        .type(SecurityScheme.Type.APIKEY)
                        .in(SecurityScheme.In.COOKIE)
                        .name("stock_access_token")
                        .description("짧게 유지되는 HttpOnly access token cookie")
                )
                .addSecuritySchemes(
                    "refresh-cookie",
                    new SecurityScheme()
                        .type(SecurityScheme.Type.APIKEY)
                        .in(SecurityScheme.In.COOKIE)
                        .name("stock_refresh_token")
                        .description("서버에 해시로 저장되고 회전되는 HttpOnly refresh token cookie")
                ));
    }
}
