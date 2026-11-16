package skinemsya.vse.ru.api.internal.pojo.factory;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;
import skinemsya.vse.ru.api.internal.enums.auth.ChatSuggestedAction;
import skinemsya.vse.ru.api.internal.enums.debts.DebtStatus;
import skinemsya.vse.ru.api.internal.enums.events.EventStatus;
import skinemsya.vse.ru.api.internal.enums.groups.GroupRole;
import skinemsya.vse.ru.api.internal.enums.groups.GroupType;
import skinemsya.vse.ru.api.internal.enums.payments.PaymentStatus;
import skinemsya.vse.ru.api.internal.pojo.auth.AuthGroup;
import skinemsya.vse.ru.api.internal.pojo.auth.AuthResult;
import skinemsya.vse.ru.api.internal.pojo.auth.ChatBootstrap;
import skinemsya.vse.ru.api.internal.pojo.auth.RefreshToken;
import skinemsya.vse.ru.api.internal.pojo.auth.TokenPair;
import skinemsya.vse.ru.api.internal.pojo.debts.Debt;
import skinemsya.vse.ru.api.internal.pojo.debts.DebtSummary;
import skinemsya.vse.ru.api.internal.pojo.events.Event;
import skinemsya.vse.ru.api.internal.pojo.events.EventParticipant;
import skinemsya.vse.ru.api.internal.pojo.files.StoredFile;
import skinemsya.vse.ru.api.internal.pojo.groups.Group;
import skinemsya.vse.ru.api.internal.pojo.groups.GroupMember;
import skinemsya.vse.ru.api.internal.pojo.groups.GroupMemberView;
import skinemsya.vse.ru.api.internal.pojo.payments.PayerReminderJob;
import skinemsya.vse.ru.api.internal.pojo.payments.Payment;
import skinemsya.vse.ru.api.internal.pojo.payments.PaymentDetailsView;
import skinemsya.vse.ru.api.internal.pojo.payments.PaymentInfo;
import skinemsya.vse.ru.api.internal.pojo.receipts.PositionData;
import skinemsya.vse.ru.api.internal.pojo.receipts.SelectionData;
import skinemsya.vse.ru.api.internal.pojo.receipts.SharedTargetData;
import skinemsya.vse.ru.api.internal.pojo.telegram.TelegramChatContext;
import skinemsya.vse.ru.api.internal.pojo.telegram.TelegramIdentity;
import skinemsya.vse.ru.api.internal.pojo.telegram.TelegramInitData;
import skinemsya.vse.ru.api.internal.pojo.telegram.TelegramSentMessage;
import skinemsya.vse.ru.api.internal.pojo.users.TelegramUserData;
import skinemsya.vse.ru.api.internal.pojo.users.User;
import skinemsya.vse.ru.api.internal.pojo.users.UserProfile;

/** Фабрика внутренних POJO для unit-тестов. */
public final class ServicePojoFactory {

    private ServicePojoFactory() {}

    /** Создаёт заполненный POJO пользователя. */
    public static User createUser() {
        return User.builder()
                .id(1L)
                .telegramUserId(100_001L)
                .displayName("Alice")
                .telegramUsername("alice")
                .createdAt(Instant.parse("2026-01-01T10:00:00Z"))
                .updatedAt(Instant.parse("2026-01-02T10:00:00Z"))
                .photoUrl("https://cdn.example/photo.jpg")
                .build();
    }

    /** Создаёт заполненный POJO профиля пользователя. */
    public static UserProfile createUserProfile() {
        return UserProfile.builder()
                .id(2L)
                .userId(1L)
                .paymentDetails("+79990000000")
                .phone("+79990000000")
                .preferredBank("Bank")
                .notificationSettings("{\"enabled\":true}")
                .build();
    }

    /** Создаёт данные пользователя Telegram. */
    public static TelegramUserData createTelegramUserData() {
        return new TelegramUserData(100_001L, "Alice", "alice", "https://cdn.example/photo.jpg");
    }

    /** Создаёт действующий POJO refresh-токена. */
    public static RefreshToken createRefreshToken(String rawToken) {
        return RefreshToken.builder()
                .id(1L)
                .userId(1L)
                .tokenHash(sha256(rawToken))
                .expiresAt(Instant.parse("2027-01-01T10:00:00Z"))
                .revoked(false)
                .createdAt(Instant.parse("2026-01-01T10:00:00Z"))
                .build();
    }

    /** Создаёт пару токенов. */
    public static TokenPair createTokenPair() {
        return new TokenPair("access-token", "refresh-token", 900L);
    }

    /** Создаёт данные группы для аутентификации. */
    public static AuthGroup createAuthGroup() {
        return new AuthGroup(10L, "Friends", GroupType.CHAT_LINKED);
    }

    /** Создаёт chat bootstrap. */
    public static ChatBootstrap createChatBootstrap() {
        return new ChatBootstrap(10L, "Friends", GroupType.CHAT_LINKED, ChatSuggestedAction.OPEN_APP, 20L);
    }

    /** Создаёт результат аутентификации с chat bootstrap. */
    public static AuthResult createAuthResult() {
        return new AuthResult(createTokenPair(), Optional.of(createChatBootstrap()));
    }

    /** Создаёт идентичность пользователя Telegram. */
    public static TelegramIdentity createTelegramIdentity() {
        return new TelegramIdentity(
                100_001L,
                "Alice",
                Instant.parse("2026-01-01T10:00:00Z"),
                "alice",
                "https://cdn.example/photo.jpg");
    }

    /** Создаёт контекст Telegram-чата. */
    public static TelegramChatContext createTelegramChatContext() {
        return new TelegramChatContext(-100_200L, "Friends Chat", "group");
    }

    /** Создаёт проверенные Telegram initData без чата и приглашений. */
    public static TelegramInitData createTelegramInitData() {
        return new TelegramInitData(createTelegramIdentity(), Optional.empty(), Optional.empty(), Optional.empty());
    }

    /** Создаёт проверенные Telegram initData с контекстом чата. */
    public static TelegramInitData createTelegramInitDataWithChat() {
        return new TelegramInitData(
                createTelegramIdentity(),
                Optional.of(createTelegramChatContext()),
                Optional.empty(),
                Optional.empty());
    }

    /** Создаёт заполненный POJO автономной группы. */
    public static Group createGroup() {
        return Group.builder()
                .id(10L)
                .name("Friends")
                .type(GroupType.STANDALONE)
                .ownerId(1L)
                .inviteCode("0123456789abcdef0123456789abcdef")
                .createdAt(Instant.parse("2026-01-01T10:00:00Z"))
                .updatedAt(Instant.parse("2026-01-02T10:00:00Z"))
                .build();
    }

    /** Создаёт заполненный POJO участника группы. */
    public static GroupMember createGroupMember() {
        return GroupMember.builder()
                .id(20L)
                .groupId(10L)
                .userId(1L)
                .role(GroupRole.OWNER)
                .joinedAt(Instant.parse("2026-01-01T10:00:00Z"))
                .build();
    }

    /** Создаёт заполненный POJO участника группы с данными пользователя. */
    public static GroupMemberView createGroupMemberView() {
        return GroupMemberView.builder()
                .id(20L)
                .groupId(10L)
                .userId(1L)
                .role(GroupRole.OWNER)
                .displayName("Alice")
                .telegramUsername("alice")
                .telegramUserId(100_001L)
                .joinedAt(Instant.parse("2026-01-01T10:00:00Z"))
                .photoUrl("https://cdn.example/photo.jpg")
                .build();
    }

    /** Создаёт заполненный POJO сбора. */
    public static Event createEvent() {
        return Event.builder()
                .id(100L)
                .groupId(10L)
                .name("Dinner")
                .description("Desc")
                .payerId(1L)
                .createdBy(1L)
                .inviteCode("0123456789abcdef0123456789abcdef")
                .status(EventStatus.DRAFT)
                .expectedParticipantCount(4)
                .createdAt(Instant.parse("2026-01-01T10:00:00Z"))
                .updatedAt(Instant.parse("2026-01-02T10:00:00Z"))
                .build();
    }

    /** Создаёт заполненный POJO участника сбора. */
    public static EventParticipant createEventParticipant() {
        return EventParticipant.builder()
                .id(30L)
                .eventId(100L)
                .userId(2L)
                .selectionCompletedAt(Instant.parse("2026-01-03T10:00:00Z"))
                .build();
    }

    /** Создаёт заполненный POJO файла. */
    public static StoredFile createStoredFile() {
        return StoredFile.builder()
                .id(70L)
                .ownerId(1L)
                .originalName("receipt.jpg")
                .mimeType("image/jpeg")
                .sizeBytes(2_048L)
                .storagePath("abc.jpg")
                .createdAt(Instant.parse("2026-01-01T10:00:00Z"))
                .build();
    }

    /** Создаёт заполненный POJO долга. */
    public static Debt createDebt() {
        return Debt.builder()
                .id(40L)
                .eventId(100L)
                .debtorId(2L)
                .creditorId(1L)
                .amountKopecks(5_000L)
                .status(DebtStatus.UNPAID)
                .createdAt(Instant.parse("2026-01-01T10:00:00Z"))
                .updatedAt(Instant.parse("2026-01-02T10:00:00Z"))
                .build();
    }

    /** Создаёт сводку долгов пользователя. */
    public static DebtSummary createDebtSummary() {
        return new DebtSummary(5_000L, 1_500L, 1L, 0L);
    }

    /** Создаёт заполненный POJO платежа. */
    public static Payment createPayment() {
        return Payment.builder()
                .id(60L)
                .debtId(40L)
                .status(PaymentStatus.DEBTOR_CONFIRMED)
                .screenshotFileId(70L)
                .debtorConfirmedAt(Instant.parse("2026-01-03T10:00:00Z"))
                .createdAt(Instant.parse("2026-01-01T10:00:00Z"))
                .updatedAt(Instant.parse("2026-01-03T10:00:00Z"))
                .build();
    }

    /** Создаёт реквизиты для оплаты долга. */
    public static PaymentDetailsView createPaymentDetailsView() {
        return new PaymentDetailsView(
                40L, 100L, 5_000L, 1L, "Alice", "+79990000000", "+79990000000", "Bank", PaymentStatus.CREATED);
    }

    /** Создаёт напоминание плательщику. */
    public static PayerReminderJob createPayerReminderJob() {
        return PayerReminderJob.builder()
                .id(80L)
                .paymentId(60L)
                .scheduledAt(Instant.parse("2026-01-04T10:00:00Z"))
                .build();
    }

    /** Создаёт краткие данные платежа. */
    public static PaymentInfo createPaymentInfo() {
        return new PaymentInfo("DEBTOR_CONFIRMED", 70L);
    }

    /** Создаёт позицию чека для расчёта долга. */
    public static PositionData createPositionData() {
        return new PositionData(50L, 10_000L, false, BigDecimal.ONE);
    }

    /** Создаёт выбор позиции участником. */
    public static SelectionData createSelectionData() {
        return new SelectionData(50L, 2L, BigDecimal.ONE);
    }

    /** Создаёт адресата общей позиции. */
    public static SharedTargetData createSharedTargetData() {
        return new SharedTargetData(50L, 2L);
    }

    /** Создаёт отправленное сообщение Telegram. */
    public static TelegramSentMessage createTelegramSentMessage() {
        return new TelegramSentMessage(77L);
    }

    private static String sha256(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(rawToken.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }
}
