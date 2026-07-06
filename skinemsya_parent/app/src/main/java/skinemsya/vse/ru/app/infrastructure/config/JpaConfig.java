package skinemsya.vse.ru.app.infrastructure.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EntityScan(basePackages = "skinemsya.vse.ru")
@EnableJpaRepositories(basePackages = "skinemsya.vse.ru")
public class JpaConfig {}
