package project.global.swagger;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Profile("server")
@Configuration
@OpenAPIDefinition(
    info = @Info(title = "ukiki API", version = "v1", description = "SSAFY 자율 프로젝트"),
//    security = @SecurityRequirement(name = "Authorization"),
    servers = {
        @Server(url="https://k10d202.p.ssafy.io/api", description = "Server Swagger")
//        @Server(url="http://k10d202.p.ssafy.io/api", description = "Server Swagger")
    }
)
//@SecurityScheme(name = "Authorization",
//    type = SecuritySchemeType.HTTP,
//    scheme = "bearer",
//    bearerFormat = "JWT",
//    in = SecuritySchemeIn.HEADER)
public class ServerConfig {
    @Bean
    public GroupedOpenApi publicApi() {
        return GroupedOpenApi.builder()
            .group("spring")
            .pathsToMatch("/**")
            .build();
    }


}

