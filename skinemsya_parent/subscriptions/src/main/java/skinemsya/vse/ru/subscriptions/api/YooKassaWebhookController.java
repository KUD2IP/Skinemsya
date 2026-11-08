package skinemsya.vse.ru.subscriptions.api;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import skinemsya.vse.ru.subscriptions.application.SubscriptionServiceImpl;

@RestController
public class YooKassaWebhookController {

    private final SubscriptionServiceImpl subscriptionService;

    public YooKassaWebhookController(SubscriptionServiceImpl subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @PostMapping("/api/v1/integrations/yookassa/webhook")
    @ResponseStatus(HttpStatus.OK)
    public void notify(@RequestBody JsonNode body) {
        String event = body.path("event").asText("");
        if (!"payment.succeeded".equals(event) && !"payment.canceled".equals(event)) {
            return;
        }
        subscriptionService.syncYooKassaPayment(body.path("object").path("id").asText(""));
    }
}
