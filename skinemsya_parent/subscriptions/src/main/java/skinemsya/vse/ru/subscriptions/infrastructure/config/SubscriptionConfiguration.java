package skinemsya.vse.ru.subscriptions.infrastructure.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({SubscriptionProperties.class, AdminProperties.class})
public class SubscriptionConfiguration {}
