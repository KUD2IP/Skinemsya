package skinemsya.vse.ru.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "skinemsya.vse.ru")
@EntityScan(basePackages = "skinemsya.vse.ru")
@EnableJpaRepositories(basePackages = "skinemsya.vse.ru")
public class SkinemsyaApplication {

    public static void main(String[] args) {
        SpringApplication.run(SkinemsyaApplication.class, args);
    }
}
