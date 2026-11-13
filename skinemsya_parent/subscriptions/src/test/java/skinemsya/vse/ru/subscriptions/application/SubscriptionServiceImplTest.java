package skinemsya.vse.ru.subscriptions.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import skinemsya.vse.ru.common.domain.DomainException;
import skinemsya.vse.ru.common.domain.ErrorCode;
import skinemsya.vse.ru.integrations.application.TelegramBotClient;
import skinemsya.vse.ru.integrations.infrastructure.config.TelegramIntegrationProperties;
import skinemsya.vse.ru.subscriptions.domain.SubscriptionProvider;
import skinemsya.vse.ru.subscriptions.infrastructure.config.AdminProperties;
import skinemsya.vse.ru.subscriptions.infrastructure.config.SubscriptionProperties;
import skinemsya.vse.ru.subscriptions.infrastructure.persistence.ParseUsageEntity;
import skinemsya.vse.ru.subscriptions.infrastructure.persistence.ParseUsageRepository;
import skinemsya.vse.ru.subscriptions.infrastructure.persistence.SubscriptionEntity;
import skinemsya.vse.ru.subscriptions.infrastructure.persistence.SubscriptionPaymentRepository;
import skinemsya.vse.ru.subscriptions.infrastructure.persistence.SubscriptionRepository;
import skinemsya.vse.ru.users.application.UserService;
import skinemsya.vse.ru.users.domain.PaymentDetails;
import skinemsya.vse.ru.users.domain.User;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceImplTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private SubscriptionPaymentRepository paymentRepository;

    @Mock
    private ParseUsageRepository parseUsageRepository;

    @Mock
    private UserService userService;

    @Mock
    private TelegramBotClient telegramBotClient;

    @Mock
    private ObjectProvider<YooKassaClient> yooKassaClient;

    @Mock
    private YooKassaClient yooKassa;

    private SubscriptionServiceImpl service;

    @BeforeEach
    void setUp() {
        var properties = new SubscriptionProperties(
                true, 4900, 30, 30, 2, new SubscriptionProperties.YooKassa("", "", "https://app", 1, false));
        var telegram = new TelegramIntegrationProperties("token", 86_400, "https://app", "", "bot", "app", false);
        service = new SubscriptionServiceImpl(
                properties,
                subscriptionRepository,
                paymentRepository,
                parseUsageRepository,
                userService,
                telegramBotClient,
                telegram,
                yooKassaClient,
                new AdminProperties("kudzip"));
    }

    @Test
    void adminParsesWithoutAPaidPeriod() {
        when(userService.findById(5L))
                .thenReturn(Optional.of(new User(5L, 50L, "Ivan", "kudzip", Instant.now(), Instant.now(), null)));

        service.authorizeParse(5L);

        verify(subscriptionRepository, never()).findByUserId(5L);
    }

    @Test
    void refusesParseWithoutActiveSubscription() {
        when(subscriptionRepository.findByUserId(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.authorizeParse(5L))
                .isInstanceOf(DomainException.class)
                .extracting(ex -> ((DomainException) ex).errorCode())
                .isEqualTo(ErrorCode.SUBSCRIPTION_REQUIRED);
    }

    @Test
    void stopsAfterDailySafetyCap() {
        var usage = new ParseUsageEntity();
        usage.setUserId(5L);
        usage.setUsageDate(LocalDate.now(ZoneOffset.UTC));
        usage.setParseCount(2);
        when(parseUsageRepository.findByUserIdAndUsageDate(any(Long.class), any(LocalDate.class)))
                .thenReturn(Optional.of(usage));

        assertThatThrownBy(() -> service.recordModelCall(5L))
                .isInstanceOf(DomainException.class)
                .hasMessage("Слишком много попыток, попробуйте позже");
        verify(parseUsageRepository, never()).save(any());
    }

    @Test
    void acceptsStarsPayloadOnlyForTheSameUser() {
        when(userService.findById(7L))
                .thenReturn(Optional.of(new User(7L, 700L, "Ann", null, Instant.now(), Instant.now(), null)));

        assertThat(service.starsCheckoutValid("sub:7:" + java.util.UUID.randomUUID(), 700L, 30))
                .isTrue();
        assertThat(service.starsCheckoutValid("sub:7:abc", 701L, 30)).isFalse();
        assertThat(service.starsCheckoutValid("sub:7:abc", 700L, 10)).isFalse();
    }

    @Test
    void activatesSubscriptionWhenYooKassaPaymentSucceeds() {
        when(yooKassaClient.getIfAvailable()).thenReturn(yooKassa);
        when(yooKassa.getPayment("pay-1"))
                .thenReturn(new YooKassaPayment("pay-1", "succeeded", null, null, "pm-1", 5L, "checkout"));
        when(paymentRepository.findByExternalId("pay-1")).thenReturn(Optional.empty());
        when(paymentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(subscriptionRepository.findByUserId(5L)).thenReturn(Optional.empty());
        when(subscriptionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.syncYooKassaPayment("pay-1");

        var saved = ArgumentCaptor.forClass(SubscriptionEntity.class);
        verify(subscriptionRepository).save(saved.capture());
        assertThat(saved.getValue().getProvider()).isEqualTo(SubscriptionProvider.YOOKASSA);
        assertThat(saved.getValue().getYookassaPaymentMethodId()).isEqualTo("pm-1");
        assertThat(saved.getValue().getCurrentPeriodEnd()).isAfter(Instant.now());
    }

    @Test
    void grantAddsDaysAfterTheCurrentPeriod() {
        when(userService.findById(5L))
                .thenReturn(Optional.of(new User(5L, 50L, "Ann", "ann", Instant.now(), Instant.now(), null)));
        var existing = new SubscriptionEntity();
        existing.setUserId(5L);
        existing.setProvider(SubscriptionProvider.YOOKASSA);
        Instant end = Instant.now().plus(Duration.ofDays(10));
        existing.setCurrentPeriodEnd(end);
        when(subscriptionRepository.findByUserId(5L)).thenReturn(Optional.of(existing));
        when(subscriptionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.grantDays(5L, 30);

        assertThat(existing.getCurrentPeriodEnd()).isAfter(end.plus(Duration.ofDays(29)));
        assertThat(existing.getProvider()).isEqualTo(SubscriptionProvider.YOOKASSA);
    }

    @Test
    void revokeEndsThePeriod() {
        var existing = new SubscriptionEntity();
        existing.setUserId(5L);
        existing.setProvider(SubscriptionProvider.YOOKASSA);
        existing.setCurrentPeriodEnd(Instant.now().plus(Duration.ofDays(10)));
        existing.setYookassaPaymentMethodId("pm-1");
        when(subscriptionRepository.findByUserId(5L)).thenReturn(Optional.of(existing));

        service.revoke(5L);

        assertThat(existing.getCurrentPeriodEnd()).isBefore(Instant.now());
        assertThat(existing.getYookassaPaymentMethodId()).isNull();
        verify(subscriptionRepository).save(existing);
    }

    @Test
    void normalizesRussianPhone() {
        assertThat(SubscriptionServiceImpl.normalizePhone("+7 (900) 123-45-67")).isEqualTo("79001234567");
        assertThat(SubscriptionServiceImpl.normalizePhone("8 900 123 45 67")).isEqualTo("79001234567");
        assertThat(SubscriptionServiceImpl.normalizePhone("123")).isNull();
    }

    @Test
    void cardCheckoutDoesNotRequirePhone() {
        var properties = new SubscriptionProperties(
                true, 4900, 30, 30, 2, new SubscriptionProperties.YooKassa("shop", "secret", "https://app", 1, true));
        var telegram = new TelegramIntegrationProperties("token", 86_400, "https://app", "", "bot", "app", false);
        var receiptService = new SubscriptionServiceImpl(
                properties,
                subscriptionRepository,
                paymentRepository,
                parseUsageRepository,
                userService,
                telegramBotClient,
                telegram,
                yooKassaClient,
                new AdminProperties("kudzip"));
        when(userService.findById(5L))
                .thenReturn(Optional.of(new User(5L, 50L, "Ivan", null, Instant.now(), Instant.now(), null)));
        when(userService.getPaymentDetails(5L)).thenReturn(new PaymentDetails(null, null, null));
        when(yooKassaClient.getIfAvailable()).thenReturn(yooKassa);
        when(yooKassa.createCheckout(eq(5L), isNull(), anyString(), eq(false)))
                .thenReturn(new YooKassaPayment("pay-9", "pending", "https://pay", "token", null, 5L, "checkout"));
        when(paymentRepository.findByExternalId("pay-9")).thenReturn(Optional.empty());

        var checkout = receiptService.checkout(5L, SubscriptionProvider.YOOKASSA, false);

        assertThat(checkout.confirmationUrl()).isEqualTo("https://pay");
    }
}
