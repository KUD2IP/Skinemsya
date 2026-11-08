package skinemsya.vse.ru.integrations.infrastructure.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(ReceiptVisionProperties.class)
public class MlServiceConfiguration {}
