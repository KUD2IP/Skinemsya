package skinemsya.vse.ru.data.entity.factory;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import skinemsya.vse.ru.api.internal.enums.debts.DebtStatus;
import skinemsya.vse.ru.api.internal.enums.events.EventStatus;
import skinemsya.vse.ru.api.internal.enums.groups.GroupRole;
import skinemsya.vse.ru.api.internal.enums.groups.GroupType;
import skinemsya.vse.ru.api.internal.enums.payments.PaymentStatus;
import skinemsya.vse.ru.data.entity.auth.RefreshTokenEntity;
import skinemsya.vse.ru.data.entity.debts.DebtEntity;
import skinemsya.vse.ru.data.entity.events.EventEntity;
import skinemsya.vse.ru.data.entity.events.EventParticipantEntity;
import skinemsya.vse.ru.data.entity.files.FileEntity;
import skinemsya.vse.ru.data.entity.groups.GroupEntity;
import skinemsya.vse.ru.data.entity.groups.GroupMemberEntity;
import skinemsya.vse.ru.data.entity.payments.PayerReminderJobEntity;
import skinemsya.vse.ru.data.entity.payments.PaymentEntity;
import skinemsya.vse.ru.data.entity.users.UserEntity;
import skinemsya.vse.ru.data.entity.users.UserProfileEntity;

/** Фабрика JPA-сущностей для unit-тестов. */
public final class EntityFactory {

    private EntityFactory() {}

    /** Создаёт заполненную сущность пользователя. */
    public static UserEntity createUserEntity() {
        var entity = new UserEntity();
        entity.setId(1L);
        entity.setTelegramUserId(100_001L);
        entity.setDisplayName("Alice");
        entity.setTelegramUsername("alice");
        entity.setCreatedAt(Instant.parse("2026-01-01T10:00:00Z"));
        entity.setUpdatedAt(Instant.parse("2026-01-02T10:00:00Z"));
        entity.setPhotoUrl("https://cdn.example/photo.jpg");
        return entity;
    }

    /** Создаёт заполненную сущность профиля пользователя. */
    public static UserProfileEntity createUserProfileEntity() {
        var entity = new UserProfileEntity();
        entity.setId(2L);
        entity.setUserId(1L);
        entity.setPaymentDetails("+79990000000");
        entity.setPhone("+79990000000");
        entity.setPreferredBank("Bank");
        entity.setNotificationSettings("{\"enabled\":true}");
        return entity;
    }

    /** Создаёт действующую сущность refresh-токена. */
    public static RefreshTokenEntity createRefreshTokenEntity(String rawToken) {
        var entity = new RefreshTokenEntity();
        entity.setId(1L);
        entity.setUserId(1L);
        entity.setTokenHash(sha256(rawToken));
        entity.setExpiresAt(Instant.parse("2027-01-01T10:00:00Z"));
        entity.setRevoked(false);
        entity.setCreatedAt(Instant.parse("2026-01-01T10:00:00Z"));
        return entity;
    }

    /** Создаёт заполненную сущность автономной группы. */
    public static GroupEntity createGroupEntity() {
        var entity = new GroupEntity();
        entity.setId(10L);
        entity.setName("Friends");
        entity.setType(GroupType.STANDALONE);
        entity.setOwnerId(1L);
        entity.setInviteCode("0123456789abcdef0123456789abcdef");
        entity.setCreatedAt(Instant.parse("2026-01-01T10:00:00Z"));
        entity.setUpdatedAt(Instant.parse("2026-01-02T10:00:00Z"));
        return entity;
    }

    /** Создаёт заполненную сущность участника группы. */
    public static GroupMemberEntity createGroupMemberEntity() {
        var entity = new GroupMemberEntity();
        entity.setId(20L);
        entity.setGroupId(10L);
        entity.setUserId(1L);
        entity.setRole(GroupRole.OWNER);
        entity.setJoinedAt(Instant.parse("2026-01-01T10:00:00Z"));
        return entity;
    }

    /** Создаёт заполненную сущность сбора. */
    public static EventEntity createEventEntity() {
        var entity = new EventEntity();
        entity.setId(100L);
        entity.setGroupId(10L);
        entity.setName("Dinner");
        entity.setDescription("Desc");
        entity.setPayerId(1L);
        entity.setCreatedBy(1L);
        entity.setInviteCode("0123456789abcdef0123456789abcdef");
        entity.setStatus(EventStatus.DRAFT);
        entity.setExpectedParticipantCount(4);
        entity.setCreatedAt(Instant.parse("2026-01-01T10:00:00Z"));
        entity.setUpdatedAt(Instant.parse("2026-01-02T10:00:00Z"));
        return entity;
    }

    /** Создаёт заполненную сущность участника сбора. */
    public static EventParticipantEntity createEventParticipantEntity() {
        var entity = new EventParticipantEntity();
        entity.setId(30L);
        entity.setEventId(100L);
        entity.setUserId(2L);
        entity.setSelectionCompletedAt(Instant.parse("2026-01-03T10:00:00Z"));
        return entity;
    }

    /** Создаёт заполненную сущность долга. */
    public static DebtEntity createDebtEntity() {
        var entity = new DebtEntity();
        entity.setId(40L);
        entity.setEventId(100L);
        entity.setDebtorId(2L);
        entity.setCreditorId(1L);
        entity.setAmountKopecks(5_000L);
        entity.setStatus(DebtStatus.UNPAID);
        entity.setCreatedAt(Instant.parse("2026-01-01T10:00:00Z"));
        entity.setUpdatedAt(Instant.parse("2026-01-02T10:00:00Z"));
        return entity;
    }

    /** Создаёт заполненную сущность файла. */
    public static FileEntity createFileEntity() {
        var entity = new FileEntity();
        entity.setId(70L);
        entity.setOwnerId(1L);
        entity.setOriginalName("receipt.jpg");
        entity.setMimeType("image/jpeg");
        entity.setSizeBytes(2_048L);
        entity.setStoragePath("abc.jpg");
        entity.setCreatedAt(Instant.parse("2026-01-01T10:00:00Z"));
        return entity;
    }

    /** Создаёт заполненную сущность платежа. */
    public static PaymentEntity createPaymentEntity() {
        var entity = new PaymentEntity();
        entity.setId(60L);
        entity.setDebtId(40L);
        entity.setStatus(PaymentStatus.DEBTOR_CONFIRMED);
        entity.setScreenshotFileId(70L);
        entity.setDebtorConfirmedAt(Instant.parse("2026-01-03T10:00:00Z"));
        entity.setCreatedAt(Instant.parse("2026-01-01T10:00:00Z"));
        entity.setUpdatedAt(Instant.parse("2026-01-03T10:00:00Z"));
        return entity;
    }

    /** Создаёт сущность напоминания плательщику. */
    public static PayerReminderJobEntity createPayerReminderJobEntity() {
        var entity = new PayerReminderJobEntity();
        entity.setId(80L);
        entity.setPaymentId(60L);
        entity.setScheduledAt(Instant.parse("2026-01-04T10:00:00Z"));
        return entity;
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
