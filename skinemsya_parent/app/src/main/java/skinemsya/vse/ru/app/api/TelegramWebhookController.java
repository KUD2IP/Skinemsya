package skinemsya.vse.ru.app.api;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import skinemsya.vse.ru.app.application.TelegramBotUpdateService;

@RestController
@RequestMapping("/api/v1/integrations/telegram")
public class TelegramWebhookController {

    private final TelegramBotUpdateService telegramBotUpdateService;

    public TelegramWebhookController(TelegramBotUpdateService telegramBotUpdateService) {
        this.telegramBotUpdateService = telegramBotUpdateService;
    }

    @PostMapping("/webhook")
    public ResponseEntity<Void> handleWebhook(@RequestBody JsonNode update) {
        telegramBotUpdateService.handleUpdate(update);
        return ResponseEntity.ok().build();
    }
}
