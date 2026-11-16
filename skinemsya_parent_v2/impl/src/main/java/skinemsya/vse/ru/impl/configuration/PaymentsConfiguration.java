package skinemsya.vse.ru.impl.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Включает планировщик напоминаний по платежам. */
@Configuration
@EnableScheduling
public class PaymentsConfiguration {}
