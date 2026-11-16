package skinemsya.vse.ru.impl.configuration;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import skinemsya.vse.ru.impl.support.properties.JwtProperties;

/** Конфигурация модуля аутентификации. */
@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class AuthConfiguration {}
