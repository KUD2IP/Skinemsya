package skinemsya.vse.ru.impl.configuration;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import skinemsya.vse.ru.impl.support.properties.ReceiptVisionProperties;

/** Конфигурация интеграции с сервисом распознавания чеков. */
@Configuration
@EnableConfigurationProperties(ReceiptVisionProperties.class)
public class MlServiceConfiguration {}
