package kz.astyq.zimran.core.configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI zimranOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Zimran API")
                        .description("Zimran backend API")
                        .version("1.0.0"));
    }
}
