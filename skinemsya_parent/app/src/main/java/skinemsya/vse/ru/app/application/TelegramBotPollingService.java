package skinemsya.vse.ru.app.application;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.annotation.PreDestroy;
import java.net.SocketTimeoutException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import skinemsya.vse.ru.integrations.application.TelegramBotClient;
import skinemsya.vse.ru.integrations.infrastructure.config.TelegramIntegrationProperties;

@Component
@ConditionalOnProperty(
        prefix = "skinemsya.telegram",
        name = "bot-polling-enabled",
        havingValue = "true",
        matchIfMissing = true)
public class TelegramBotPollingService {

    private static final Logger log = LoggerFactory.getLogger(TelegramBotPollingService.class);
    private static final int POLL_TIMEOUT_SECONDS = 30;

    private final TelegramIntegrationProperties properties;
    private final TelegramBotClient telegramBotClient;
    private final TelegramBotUpdateService updateService;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "telegram-bot-polling");
        thread.setDaemon(true);
        return thread;
    });

    private volatile long offset;

    public TelegramBotPollingService(
            TelegramIntegrationProperties properties,
            TelegramBotClient telegramBotClient,
            TelegramBotUpdateService updateService) {
        this.properties = properties;
        this.telegramBotClient = telegramBotClient;
        this.updateService = updateService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void startPolling() {
        if (!properties.isBotPollingEnabled()) {
            log.info("Telegram bot polling is disabled");
            return;
        }
        if (properties.botToken() == null || properties.botToken().isBlank()) {
            log.warn("Telegram bot token is not configured; polling disabled");
            return;
        }

        try {
            telegramBotClient.initialize();
            telegramBotClient.deleteWebhook();
            telegramBotClient.setMyCommands();
            log.info("Telegram bot commands registered");
        } catch (Exception ex) {
            log.error("Failed to initialize Telegram bot polling", ex);
        }

        running.set(true);
        executor.submit(this::pollLoop);
        log.info("Telegram bot polling started");
    }

    @PreDestroy
    public void stopPolling() {
        running.set(false);
        executor.shutdownNow();
    }

    private void pollLoop() {
        while (running.get()) {
            try {
                JsonNode updates = telegramBotClient.getUpdates(offset, POLL_TIMEOUT_SECONDS);
                if (!updates.isArray()) {
                    continue;
                }
                for (JsonNode update : updates) {
                    offset = update.path("update_id").asLong(offset) + 1;
                    try {
                        updateService.handleUpdate(update);
                    } catch (Exception ex) {
                        log.error("Failed to handle Telegram update", ex);
                    }
                }
            } catch (Exception ex) {
                if (!running.get()) {
                    continue;
                }
                if (isBenignPollingError(ex)) {
                    log.debug("Telegram long poll timed out without updates");
                    continue;
                }
                log.error("Telegram polling error", ex);
                sleepQuietly(3_000);
            }
        }
    }

    private static void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    private static boolean isBenignPollingError(Throwable ex) {
        Throwable current = ex;
        while (current != null) {
            if (current instanceof SocketTimeoutException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
