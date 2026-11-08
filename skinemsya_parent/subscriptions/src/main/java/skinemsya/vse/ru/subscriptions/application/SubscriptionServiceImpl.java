package skinemsya.vse.ru.subscriptions.application;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import skinemsya.vse.ru.common.domain.DomainException;
import skinemsya.vse.ru.common.domain.ErrorCode;
import skinemsya.vse.ru.integrations.application.TelegramBotClient;
import skinemsya.vse.ru.integrations.infrastructure.config.TelegramIntegrationProperties;
import skinemsya.vse.ru.receipts.application.ReceiptParseAccess;
import skinemsya.vse.ru.subscriptions.domain.SubscriptionCheckout;
import skinemsya.vse.ru.subscriptions.domain.SubscriptionPaymentKind;
import skinemsya.vse.ru.subscriptions.domain.SubscriptionPaymentStatus;
import skinemsya.vse.ru.subscriptions.domain.SubscriptionProvider;
import skinemsya.vse.ru.subscriptions.domain.SubscriptionView;
import skinemsya.vse.ru.subscriptions.infrastructure.config.AdminProperties;
import skinemsya.vse.ru.subscriptions.infrastructure.config.SubscriptionProperties;
import skinemsya.vse.ru.subscriptions.infrastructure.persistence.ParseUsageEntity;
import skinemsya.vse.ru.subscriptions.infrastructure.persistence.ParseUsageRepository;
import skinemsya.vse.ru.subscriptions.infrastructure.persistence.SubscriptionEntity;
import skinemsya.vse.ru.subscriptions.infrastructure.persistence.SubscriptionPaymentEntity;
import skinemsya.vse.ru.subscriptions.infrastructure.persistence.SubscriptionPaymentRepository;
import skinemsya.vse.ru.subscriptions.infrastructure.persistence.SubscriptionRepository;
import skinemsya.vse.ru.users.application.UserService;

@Service
@Transactional
public class SubscriptionServiceImpl implements ReceiptParseAccess {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionServiceImpl.class);
    private static final Pattern STARS_PAYLOAD = Pattern.compile("sub:(\\d+):[A-Za-z0-9-]+");
    private final ConcurrentHashMap<Long, Instant> yooKassaSyncAt = new ConcurrentHashMap<>();

    private final SubscriptionProperties properties;
    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionPaymentRepository paymentRepository;
    private final ParseUsageRepository parseUsageRepository;
    private final UserService userService;
    private final TelegramBotClient telegramBotClient;
    private final TelegramIntegrationProperties telegramProperties;
    private final ObjectProvider<YooKassaClient> yooKassaClient;
    private final AdminProperties adminProperties;

    public SubscriptionServiceImpl(
            SubscriptionProperties properties,
            SubscriptionRepository subscriptionRepository,
            SubscriptionPaymentRepository paymentRepository,
            ParseUsageRepository parseUsageRepository,
            UserService userService,
            TelegramBotClient telegramBotClient,
            TelegramIntegrationProperties telegramProperties,
            ObjectProvider<YooKassaClient> yooKassaClient,
            AdminProperties adminProperties) {
        this.properties = properties;
        this.subscriptionRepository = subscriptionRepository;
        this.paymentRepository = paymentRepository;
        this.parseUsageRepository = parseUsageRepository;
        this.userService = userService;
        this.telegramBotClient = telegramBotClient;
        this.telegramProperties = telegramProperties;
        this.yooKassaClient = yooKassaClient;
        this.adminProperties = adminProperties;
    }

    @Transactional(readOnly = true)
    public SubscriptionView current(long userId) {
        SubscriptionEntity entity = subscriptionRepository.findByUserId(userId).orElse(null);
        boolean active = !properties.enforced() || isActive(entity, Instant.now()) || adminUser(userId);
        return new SubscriptionView(
                active,
                entity == null ? null : entity.getCurrentPeriodEnd(),
                entity != null && entity.isCancelAtPeriodEnd(),
                entity == null ? null : entity.getProvider(),
                properties.priceKopecks(),
                properties.stars(),
                yooKassaClient.getIfAvailable() != null,
                starsConfigured());
    }

    public SubscriptionCheckout checkout(long userId, SubscriptionProvider provider, boolean sbp) {
        userService.findById(userId).orElseThrow(() -> new DomainException(ErrorCode.NOT_FOUND, "User not found"));
        if (provider == SubscriptionProvider.STARS) {
            return startStars(userId);
        }
        return startYooKassa(userId, sbp);
    }

    public void grantDays(long userId, int days) {
        if (days < 1 || days > 366) {
            throw new DomainException(ErrorCode.VALIDATION_ERROR, "Срок подписки от 1 до 366 дней");
        }
        userService.findById(userId).orElseThrow(() -> new DomainException(ErrorCode.NOT_FOUND, "User not found"));
        Instant now = Instant.now();
        SubscriptionEntity subscription = subscriptionOrNew(userId, SubscriptionProvider.YOOKASSA, now);
        Instant base = subscription.getCurrentPeriodEnd() != null
                        && subscription.getCurrentPeriodEnd().isAfter(now)
                ? subscription.getCurrentPeriodEnd()
                : now;
        subscription.setProvider(SubscriptionProvider.YOOKASSA);
        subscription.setCurrentPeriodEnd(base.plus(Duration.ofDays(days)));
        subscription.setCancelAtPeriodEnd(false);
        subscription.setUpdatedAt(now);
        subscriptionRepository.save(subscription);
    }

    public void cancel(long userId) {
        SubscriptionEntity subscription = subscriptionRepository
                .findByUserId(userId)
                .orElseThrow(() -> new DomainException(ErrorCode.NOT_FOUND, "Подписка не найдена"));
        if (!isActive(subscription, Instant.now())) {
            throw new DomainException(ErrorCode.DOMAIN_RULE_VIOLATION, "Подписка уже не действует");
        }
        if (subscription.getProvider() == SubscriptionProvider.STARS && subscription.getStarsChargeId() != null) {
            long telegramUserId = userService
                    .findById(userId)
                    .orElseThrow(() -> new DomainException(ErrorCode.NOT_FOUND, "User not found"))
                    .telegramUserId();
            try {
                telegramBotClient.cancelStarSubscription(telegramUserId, subscription.getStarsChargeId());
            } catch (RuntimeException ex) {
                throw new DomainException(
                        ErrorCode.INTEGRATION_ERROR,
                        "Не удалось отменить продление в Telegram. Откройте настройки подписок Telegram",
                        ex);
            }
        }
        subscription.setCancelAtPeriodEnd(true);
        subscription.setUpdatedAt(Instant.now());
        subscriptionRepository.save(subscription);
    }

    public void revoke(long userId) {
        SubscriptionEntity subscription = subscriptionRepository
                .findByUserId(userId)
                .orElseThrow(() -> new DomainException(ErrorCode.NOT_FOUND, "Подписка не найдена"));
        if (subscription.getProvider() == SubscriptionProvider.STARS && subscription.getStarsChargeId() != null) {
            long telegramUserId = userService
                    .findById(userId)
                    .orElseThrow(() -> new DomainException(ErrorCode.NOT_FOUND, "User not found"))
                    .telegramUserId();
            try {
                telegramBotClient.cancelStarSubscription(telegramUserId, subscription.getStarsChargeId());
            } catch (RuntimeException ex) {
                throw new DomainException(ErrorCode.INTEGRATION_ERROR, "Не удалось отменить продление в Telegram", ex);
            }
        }
        Instant now = Instant.now();
        subscription.setCurrentPeriodEnd(now.minusSeconds(1));
        subscription.setCancelAtPeriodEnd(true);
        subscription.setYookassaPaymentMethodId(null);
        subscription.setUpdatedAt(now);
        subscriptionRepository.save(subscription);
    }

    public void handlePreCheckout(JsonNode query) {
        boolean ok = "XTR".equals(query.path("currency").asText())
                && starsCheckoutValid(
                        query.path("invoice_payload").asText(""),
                        query.path("from").path("id").asLong(),
                        query.path("total_amount").asInt());
        telegramBotClient.answerPreCheckoutQuery(
                query.path("id").asText(""),
                ok,
                ok ? null : "Не удалось подтвердить подписку. Откройте оплату ещё раз.");
    }

    public void handleStarsPayment(JsonNode message) {
        JsonNode payment = message.path("successful_payment");
        if (!"XTR".equals(payment.path("currency").asText())) {
            return;
        }
        String chargeId = payment.path("telegram_payment_charge_id").asText("");
        if (chargeId.isBlank() || paymentRepository.findByExternalId(chargeId).isPresent()) {
            return;
        }
        String payload = payment.path("invoice_payload").asText("");
        long telegramUserId = message.path("from").path("id").asLong();
        if (!starsCheckoutValid(
                payload, telegramUserId, payment.path("total_amount").asInt())) {
            return;
        }
        long userId = Long.parseLong(payload.split(":")[1]);
        long expiration = payment.path("subscription_expiration_date").asLong(0);
        Instant periodEnd = expiration > 0
                ? Instant.ofEpochSecond(expiration)
                : Instant.now().plus(Duration.ofDays(properties.periodDays()));

        var row = new SubscriptionPaymentEntity();
        row.setUserId(userId);
        row.setProvider(SubscriptionProvider.STARS);
        row.setExternalId(chargeId);
        row.setAmountKopecks(0);
        row.setStatus(SubscriptionPaymentStatus.SUCCEEDED);
        row.setKind(
                payment.path("is_first_recurring").asBoolean(true)
                        ? SubscriptionPaymentKind.CHECKOUT
                        : SubscriptionPaymentKind.RENEWAL);
        row.setCreatedAt(Instant.now());
        paymentRepository.save(row);
        activateStars(userId, periodEnd, chargeId);
    }

    public void syncYooKassaPayment(String paymentId) {
        YooKassaClient client = yooKassaClient.getIfAvailable();
        if (client == null || paymentId == null || paymentId.isBlank()) {
            return;
        }
        applyRemote(client.getPayment(paymentId));
    }

    public void syncPendingYooKassa(long userId) {
        YooKassaClient client = yooKassaClient.getIfAvailable();
        if (client == null) {
            return;
        }
        Instant now = Instant.now();
        Instant previous = yooKassaSyncAt.get(userId);
        if (previous != null && previous.plusSeconds(3).isAfter(now)) {
            return;
        }
        yooKassaSyncAt.put(userId, now);
        List<SubscriptionPaymentEntity> pending =
                paymentRepository.findTop3ByUserIdAndProviderAndStatusAndCreatedAtAfterOrderByCreatedAtDesc(
                        userId,
                        SubscriptionProvider.YOOKASSA,
                        SubscriptionPaymentStatus.PENDING,
                        now.minus(Duration.ofHours(2)));
        for (SubscriptionPaymentEntity payment : pending) {
            try {
                applyRemote(client.getPayment(payment.getExternalId()));
            } catch (RuntimeException ex) {
                log.warn("YooKassa payment refresh failed for {}", payment.getExternalId());
            }
        }
    }

    @Transactional(readOnly = true)
    public List<Long> dueRenewalIds() {
        if (yooKassaClient.getIfAvailable() == null) {
            return List.of();
        }
        Instant now = Instant.now();
        return subscriptionRepository
                .findDue(SubscriptionProvider.YOOKASSA, now.minus(Duration.ofDays(3)), now.plus(Duration.ofDays(1)))
                .stream()
                .map(SubscriptionEntity::getId)
                .toList();
    }

    public void renew(long subscriptionId) {
        YooKassaClient client = yooKassaClient.getIfAvailable();
        if (client == null) {
            return;
        }
        SubscriptionEntity subscription =
                subscriptionRepository.findById(subscriptionId).orElse(null);
        if (subscription == null
                || subscription.isCancelAtPeriodEnd()
                || subscription.getYookassaPaymentMethodId() == null
                || subscription.getCurrentPeriodEnd() == null) {
            return;
        }
        if (paymentRepository.existsByUserIdAndKindAndStatusAndCreatedAtAfter(
                subscription.getUserId(),
                SubscriptionPaymentKind.RENEWAL,
                SubscriptionPaymentStatus.PENDING,
                Instant.now().minus(Duration.ofHours(20)))) {
            return;
        }
        String phone = phoneOf(subscription.getUserId());
        if (properties.yookassa().receiptEnabled() && phone == null) {
            log.warn(
                    "Skip subscription renewal for user {}: phone is required for the receipt",
                    subscription.getUserId());
            return;
        }
        String key = "renew-" + subscription.getUserId() + "-"
                + subscription.getCurrentPeriodEnd().getEpochSecond();
        applyRemote(
                client.createRenewal(subscription.getUserId(), subscription.getYookassaPaymentMethodId(), phone, key));
    }

    @Override
    public void authorizeParse(long userId) {
        if (!properties.enforced() || adminUser(userId)) {
            return;
        }
        SubscriptionEntity subscription =
                subscriptionRepository.findByUserId(userId).orElse(null);
        if (!isActive(subscription, Instant.now())) {
            throw new DomainException(
                    ErrorCode.SUBSCRIPTION_REQUIRED, "Чтобы прочитать чек, оформите распознавание на 30 дней");
        }
    }

    @Override
    public void recordModelCall(long userId) {
        if (!properties.enforced()) {
            return;
        }
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        ParseUsageEntity usage = parseUsageRepository
                .findByUserIdAndUsageDate(userId, today)
                .orElseGet(() -> {
                    var row = new ParseUsageEntity();
                    row.setUserId(userId);
                    row.setUsageDate(today);
                    row.setParseCount(0);
                    return row;
                });
        if (usage.getParseCount() >= properties.maxParsesPerDay()) {
            throw new DomainException(ErrorCode.DOMAIN_RULE_VIOLATION, "Слишком много попыток, попробуйте позже");
        }
        usage.setParseCount(usage.getParseCount() + 1);
        parseUsageRepository.save(usage);
    }

    private SubscriptionCheckout startYooKassa(long userId, boolean sbp) {
        YooKassaClient client = yooKassaClient.getIfAvailable();
        if (client == null) {
            throw new DomainException(ErrorCode.DOMAIN_RULE_VIOLATION, "Оплата картой сейчас недоступна");
        }
        String phone = phoneOf(userId);
        if (properties.yookassa().receiptEnabled() && phone == null) {
            throw new DomainException(
                    ErrorCode.DOMAIN_RULE_VIOLATION, "Добавьте телефон в профиле — он нужен для чека");
        }
        YooKassaPayment payment =
                client.createCheckout(userId, phone, "checkout-" + userId + "-" + UUID.randomUUID(), sbp);
        applyRemote(payment);
        return new SubscriptionCheckout(
                "YOOKASSA", payment.confirmationUrl(), payment.confirmationToken(), payment.id(), null);
    }

    private SubscriptionCheckout startStars(long userId) {
        if (!starsConfigured()) {
            throw new DomainException(ErrorCode.DOMAIN_RULE_VIOLATION, "Оплата звёздами сейчас недоступна");
        }
        String payload = "sub:" + userId + ":" + UUID.randomUUID();
        String link = telegramBotClient.createStarsSubscriptionLink(
                "Распознавание чеков",
                "30 дней чтения фото чеков. Дальше подписка продлевается, отмена в профиле.",
                payload,
                properties.stars());
        return new SubscriptionCheckout("STARS", null, null, null, link);
    }

    private void applyRemote(YooKassaPayment remote) {
        if (remote.id() == null || remote.id().isBlank()) {
            return;
        }
        SubscriptionPaymentEntity local =
                paymentRepository.findByExternalId(remote.id()).orElse(null);
        long userId;
        if (local != null) {
            if (local.getStatus() == SubscriptionPaymentStatus.SUCCEEDED) {
                return;
            }
            userId = local.getUserId();
        } else if (remote.userId() == null) {
            return;
        } else {
            userId = remote.userId();
            local = new SubscriptionPaymentEntity();
            local.setUserId(userId);
            local.setProvider(SubscriptionProvider.YOOKASSA);
            local.setExternalId(remote.id());
            local.setAmountKopecks(properties.priceKopecks());
            local.setKind(
                    "renewal".equals(remote.kind())
                            ? SubscriptionPaymentKind.RENEWAL
                            : SubscriptionPaymentKind.CHECKOUT);
            local.setCreatedAt(Instant.now());
        }
        if ("succeeded".equals(remote.status())) {
            local.setStatus(SubscriptionPaymentStatus.SUCCEEDED);
            paymentRepository.save(local);
            activateYooKassa(userId, remote.paymentMethodId());
            return;
        }
        if ("canceled".equals(remote.status())) {
            local.setStatus(SubscriptionPaymentStatus.CANCELED);
            paymentRepository.save(local);
            return;
        }
        local.setStatus(SubscriptionPaymentStatus.PENDING);
        paymentRepository.save(local);
    }

    private void activateYooKassa(long userId, String paymentMethodId) {
        Instant now = Instant.now();
        SubscriptionEntity subscription = subscriptionOrNew(userId, SubscriptionProvider.YOOKASSA, now);
        Instant base = subscription.getCurrentPeriodEnd() != null
                        && subscription.getCurrentPeriodEnd().isAfter(now)
                ? subscription.getCurrentPeriodEnd()
                : now;
        subscription.setProvider(SubscriptionProvider.YOOKASSA);
        subscription.setCurrentPeriodEnd(base.plus(Duration.ofDays(properties.periodDays())));
        subscription.setCancelAtPeriodEnd(false);
        if (paymentMethodId != null && !paymentMethodId.isBlank()) {
            subscription.setYookassaPaymentMethodId(paymentMethodId);
        }
        subscription.setUpdatedAt(now);
        subscriptionRepository.save(subscription);
    }

    private void activateStars(long userId, Instant periodEnd, String chargeId) {
        Instant now = Instant.now();
        SubscriptionEntity subscription = subscriptionOrNew(userId, SubscriptionProvider.STARS, now);
        Instant end = periodEnd;
        if (subscription.getCurrentPeriodEnd() != null
                && subscription.getCurrentPeriodEnd().isAfter(end)) {
            end = subscription.getCurrentPeriodEnd();
        }
        subscription.setProvider(SubscriptionProvider.STARS);
        subscription.setCurrentPeriodEnd(end);
        subscription.setCancelAtPeriodEnd(false);
        subscription.setStarsChargeId(chargeId);
        subscription.setUpdatedAt(now);
        subscriptionRepository.save(subscription);
    }

    private SubscriptionEntity subscriptionOrNew(long userId, SubscriptionProvider provider, Instant now) {
        return subscriptionRepository.findByUserId(userId).orElseGet(() -> {
            var created = new SubscriptionEntity();
            created.setUserId(userId);
            created.setProvider(provider);
            created.setCreatedAt(now);
            created.setUpdatedAt(now);
            return created;
        });
    }

    private String phoneOf(long userId) {
        try {
            return normalizePhone(userService.getPaymentDetails(userId).phone());
        } catch (DomainException ex) {
            return null;
        }
    }

    static String normalizePhone(String phone) {
        if (phone == null) {
            return null;
        }
        String digits = phone.replaceAll("\\D", "");
        if (digits.length() == 11 && digits.startsWith("8")) {
            digits = "7" + digits.substring(1);
        }
        if (digits.length() == 10) {
            digits = "7" + digits;
        }
        return digits.length() >= 11 ? digits : null;
    }

    boolean starsCheckoutValid(String payload, long telegramUserId, int amount) {
        Matcher matcher = STARS_PAYLOAD.matcher(payload == null ? "" : payload);
        if (!matcher.matches() || amount != properties.stars()) {
            return false;
        }
        long userId = Long.parseLong(matcher.group(1));
        return userService
                .findById(userId)
                .filter(user -> user.telegramUserId() == telegramUserId)
                .isPresent();
    }

    private boolean starsConfigured() {
        return telegramProperties.botToken() != null
                && !telegramProperties.botToken().isBlank();
    }

    private boolean adminUser(long userId) {
        var found = userService.findById(userId);
        if (found == null || found.isEmpty()) {
            return false;
        }
        return adminProperties.matches(found.get().telegramUsername());
    }

    private static boolean isActive(SubscriptionEntity entity, Instant now) {
        return entity != null
                && entity.getCurrentPeriodEnd() != null
                && entity.getCurrentPeriodEnd().isAfter(now);
    }
}
