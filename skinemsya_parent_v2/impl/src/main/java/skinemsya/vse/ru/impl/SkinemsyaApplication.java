package skinemsya.vse.ru.impl;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Точка запуска приложения Skinemsya. */
@SpringBootApplication(scanBasePackages = "skinemsya.vse.ru")
public class SkinemsyaApplication {

    /** Запускает Spring Boot приложение. */
    public static void main(String[] args) {
        SpringApplication.run(SkinemsyaApplication.class, args);
    }
}
