package skinemsya.vse.ru.subscriptions.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class SubscriptionRenewalJob {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionRenewalJob.class);

    private final SubscriptionServiceImpl subscriptionService;

    public SubscriptionRenewalJob(SubscriptionServiceImpl subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @Scheduled(cron = "0 15 * * * *")
    public void renewDue() {
        for (Long subscriptionId : subscriptionService.dueRenewalIds()) {
            try {
                subscriptionService.renew(subscriptionId);
            } catch (RuntimeException ex) {
                log.warn("Subscription renewal failed for {}", subscriptionId);
            }
        }
    }
}
