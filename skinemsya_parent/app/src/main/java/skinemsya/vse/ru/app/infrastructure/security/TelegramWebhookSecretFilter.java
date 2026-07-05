package skinemsya.vse.ru.app.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import skinemsya.vse.ru.integrations.infrastructure.config.TelegramIntegrationProperties;

@Component
@Profile("!webmvc-test")
public class TelegramWebhookSecretFilter extends OncePerRequestFilter {

    private static final String WEBHOOK_PATH = "/api/v1/integrations/telegram/webhook";
    private static final String SECRET_HEADER = "X-Telegram-Bot-Api-Secret-Token";

    private final TelegramIntegrationProperties telegramIntegrationProperties;

    public TelegramWebhookSecretFilter(TelegramIntegrationProperties telegramIntegrationProperties) {
        this.telegramIntegrationProperties = telegramIntegrationProperties;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !WEBHOOK_PATH.equals(request.getRequestURI()) || !telegramIntegrationProperties.isWebhookProtected();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String providedSecret = request.getHeader(SECRET_HEADER);
        if (!telegramIntegrationProperties.webhookSecret().equals(providedSecret)) {
            response.setStatus(HttpStatus.FORBIDDEN.value());
            return;
        }
        filterChain.doFilter(request, response);
    }
}
