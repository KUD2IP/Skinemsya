package skinemsya.vse.ru.app.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI skinemsyaOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Skinemsya API")
                        .description("Telegram Mini App backend for expense splitting")
                        .version("1.0.0-SNAPSHOT"));
    }
}
