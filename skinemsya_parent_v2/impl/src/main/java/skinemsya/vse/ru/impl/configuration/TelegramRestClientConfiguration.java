package skinemsya.vse.ru.impl.configuration;

import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

/** Конфигурация HTTP-клиента Telegram Bot API. */
@Configuration
public class TelegramRestClientConfiguration {

    /** Must exceed Telegram long-poll timeout (30s) plus network buffer. */
    private static final int READ_TIMEOUT_MS = 45_000;

    /** Настраивает таймауты {@link org.springframework.web.client.RestClient} для long polling. */
    @Bean
    RestClientCustomizer telegramRestClientCustomizer() {
        return builder -> {
            var requestFactory = new SimpleClientHttpRequestFactory();
            requestFactory.setConnectTimeout(5_000);
            requestFactory.setReadTimeout(READ_TIMEOUT_MS);
            builder.requestFactory(requestFactory);
        };
    }
}
