package skinemsya.vse.ru.impl.configuration;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import skinemsya.vse.ru.impl.support.properties.TelegramIntegrationProperties;

/** Конфигурация интеграции с Telegram. */
@Configuration
@EnableConfigurationProperties(TelegramIntegrationProperties.class)
public class IntegrationsConfiguration {}
