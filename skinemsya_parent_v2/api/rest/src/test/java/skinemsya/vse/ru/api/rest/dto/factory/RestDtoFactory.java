package skinemsya.vse.ru.api.rest.dto.factory;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import skinemsya.vse.ru.api.rest.auth.dto.ChatBootstrapResponse;
import skinemsya.vse.ru.api.rest.auth.dto.RefreshTokenRequest;
import skinemsya.vse.ru.api.rest.auth.dto.TelegramAuthRequest;
import skinemsya.vse.ru.api.rest.auth.dto.TokenResponse;
import skinemsya.vse.ru.api.rest.debts.dto.DebtResponse;
import skinemsya.vse.ru.api.rest.debts.dto.DebtSummaryResponse;
import skinemsya.vse.ru.api.rest.debts.dto.ParticipantStatusItem;
import skinemsya.vse.ru.api.rest.debts.dto.ParticipantsStatusResponse;
import skinemsya.vse.ru.api.rest.events.dto.CreateEventRequest;
import skinemsya.vse.ru.api.rest.events.dto.EventResponse;
import skinemsya.vse.ru.api.rest.events.dto.UpdateExpectedParticipantsRequest;
import skinemsya.vse.ru.api.rest.files.dto.FileResponse;
import skinemsya.vse.ru.api.rest.groups.dto.GroupMemberViewResponse;
import skinemsya.vse.ru.api.rest.groups.dto.GroupResponse;
import skinemsya.vse.ru.api.rest.groups.dto.InviteLinkResponse;
import skinemsya.vse.ru.api.rest.payments.dto.ConfirmDebtorRequest;
import skinemsya.vse.ru.api.rest.payments.dto.PaymentDetailsResponse;
import skinemsya.vse.ru.api.rest.payments.dto.PaymentResponse;
import skinemsya.vse.ru.api.rest.users.dto.UpdateProfileRequest;
import skinemsya.vse.ru.api.rest.users.dto.UserResponse;

/** Фабрика REST DTO для unit-тестов. */
public final class RestDtoFactory {

    private RestDtoFactory() {}

    /** Создаёт заполненный ответ с данными пользователя. */
    public static UserResponse createUserResponse() {
        return new UserResponse()
                .id(1L)
                .telegramUserId(100_001L)
                .displayName("Alice")
                .paymentDetails("+79990000000")
                .phone("+79990000000")
                .preferredBank("Bank")
                .notificationSettings("{\"enabled\":true}")
                .photoUrl("https://cdn.example/photo.jpg");
    }

    /** Создаёт запрос на обновление профиля. */
    public static UpdateProfileRequest createUpdateProfileRequest() {
        return new UpdateProfileRequest()
                .paymentDetails("+79990000000")
                .phone("+79990000000")
                .preferredBank("Bank")
                .notificationSettings("{\"enabled\":true}");
    }

    /** Создаёт запрос аутентификации через Telegram initData. */
    public static TelegramAuthRequest createTelegramAuthRequest() {
        return new TelegramAuthRequest()
                .initData("query_id=AAE&user=%7B%22id%22%3A100001%7D&auth_date=1767223200&hash=abc");
    }

    /** Создаёт запрос на обновление пары токенов. */
    public static RefreshTokenRequest createRefreshTokenRequest() {
        return new RefreshTokenRequest().refreshToken("refresh-token");
    }

    /** Создаёт ответ с парой токенов. */
    public static TokenResponse createTokenResponse() {
        return new TokenResponse()
                .accessToken("access-token")
                .refreshToken("refresh-token")
                .expiresIn(900L);
    }

    /** Создаёт chat bootstrap в ответе аутентификации. */
    public static ChatBootstrapResponse createChatBootstrapResponse() {
        return new ChatBootstrapResponse()
                .groupId(10L)
                .groupName("Friends")
                .groupType(ChatBootstrapResponse.GroupTypeEnum.CHAT_LINKED)
                .suggestedAction(ChatBootstrapResponse.SuggestedActionEnum.OPEN_APP)
                .eventId(20L);
    }

    /** Создаёт заполненный ответ с данными группы. */
    public static GroupResponse createGroupResponse() {
        return new GroupResponse()
                .id(10L)
                .name("Friends")
                .type(GroupResponse.TypeEnum.STANDALONE)
                .ownerId(1L)
                .createdAt(OffsetDateTime.of(2026, 1, 1, 10, 0, 0, 0, ZoneOffset.UTC))
                .updatedAt(OffsetDateTime.of(2026, 1, 2, 10, 0, 0, 0, ZoneOffset.UTC));
    }

    /** Создаёт заполненный ответ с данными участника группы. */
    public static GroupMemberViewResponse createGroupMemberViewResponse() {
        return new GroupMemberViewResponse()
                .id(20L)
                .groupId(10L)
                .userId(1L)
                .role(GroupMemberViewResponse.RoleEnum.OWNER)
                .displayName("Alice")
                .telegramUsername("alice")
                .telegramUserId(100_001L)
                .joinedAt(OffsetDateTime.of(2026, 1, 1, 10, 0, 0, 0, ZoneOffset.UTC))
                .photoUrl("https://cdn.example/photo.jpg");
    }

    /** Создаёт запрос на создание сбора. */
    public static CreateEventRequest createCreateEventRequest() {
        return new CreateEventRequest().name("Dinner").description("Desc").payerId(1L).expectedParticipantCount(4);
    }

    /** Создаёт запрос на изменение ожидаемого числа участников. */
    public static UpdateExpectedParticipantsRequest createUpdateExpectedParticipantsRequest() {
        return new UpdateExpectedParticipantsRequest().expectedParticipantCount(6);
    }

    /** Создаёт заполненный ответ со сбором. */
    public static EventResponse createEventResponse() {
        return new EventResponse()
                .id(100L)
                .groupId(10L)
                .name("Dinner")
                .description("Desc")
                .payerId(1L)
                .createdBy(1L)
                .status(EventResponse.StatusEnum.DRAFT)
                .payerRequisitesReady(true)
                .expectedParticipantCount(4)
                .joinedCount(2L)
                .currentUserJoined(true)
                .createdAt(OffsetDateTime.of(2026, 1, 1, 10, 0, 0, 0, ZoneOffset.UTC))
                .updatedAt(OffsetDateTime.of(2026, 1, 2, 10, 0, 0, 0, ZoneOffset.UTC));
    }

    /** Создаёт заполненный ответ с долгом. */
    public static DebtResponse createDebtResponse() {
        return new DebtResponse()
                .id(40L)
                .eventId(100L)
                .debtorId(2L)
                .creditorId(1L)
                .amountKopecks(5_000L)
                .status(DebtResponse.StatusEnum.UNPAID)
                .paymentStatus("DEBTOR_CONFIRMED")
                .screenshotFileId(70L)
                .createdAt(OffsetDateTime.of(2026, 1, 1, 10, 0, 0, 0, ZoneOffset.UTC))
                .updatedAt(OffsetDateTime.of(2026, 1, 2, 10, 0, 0, 0, ZoneOffset.UTC));
    }

    /** Создаёт сводку долгов в REST-ответе. */
    public static DebtSummaryResponse createDebtSummaryResponse() {
        return new DebtSummaryResponse()
                .totalOwedKopecks(5_000L)
                .totalToReceiveKopecks(1_500L)
                .unpaidCount(1L)
                .pendingCount(0L);
    }

    /** Создаёт статус участников сбора. */
    public static ParticipantsStatusResponse createParticipantsStatusResponse() {
        return new ParticipantsStatusResponse()
                .totalParticipants(2L)
                .expectedParticipantCount(4L)
                .joinedCount(2L)
                .completedSelections(1L)
                .participants(List.of(new ParticipantStatusItem()
                        .userId(2L)
                        .selectionCompleted(true)
                        .debtStatus("UNPAID")));
    }

    /** Создаёт заполненный ответ с файлом. */
    public static FileResponse createFileResponse() {
        return new FileResponse()
                .id(70L)
                .originalName("receipt.jpg")
                .mimeType("image/jpeg")
                .sizeBytes(2_048L)
                .createdAt(OffsetDateTime.of(2026, 1, 1, 10, 0, 0, 0, ZoneOffset.UTC));
    }

    /** Создаёт запрос должника на подтверждение перевода. */
    public static ConfirmDebtorRequest createConfirmDebtorRequest() {
        return new ConfirmDebtorRequest().screenshotFileId(70L);
    }

    /** Создаёт заполненный ответ с платежом. */
    public static PaymentResponse createPaymentResponse() {
        return new PaymentResponse()
                .id(60L)
                .debtId(40L)
                .status(PaymentResponse.StatusEnum.DEBTOR_CONFIRMED)
                .screenshotFileId(70L)
                .debtorConfirmedAt(OffsetDateTime.of(2026, 1, 3, 10, 0, 0, 0, ZoneOffset.UTC))
                .createdAt(OffsetDateTime.of(2026, 1, 1, 10, 0, 0, 0, ZoneOffset.UTC))
                .updatedAt(OffsetDateTime.of(2026, 1, 3, 10, 0, 0, 0, ZoneOffset.UTC));
    }

    /** Создаёт ответ с реквизитами для оплаты. */
    public static PaymentDetailsResponse createPaymentDetailsResponse() {
        return new PaymentDetailsResponse()
                .debtId(40L)
                .eventId(100L)
                .amountKopecks(5_000L)
                .creditorId(1L)
                .creditorName("Alice")
                .paymentDetails("+79990000000")
                .phone("+79990000000")
                .preferredBank("Bank")
                .status(PaymentDetailsResponse.StatusEnum.CREATED);
    }

    /** Создаёт ответ со ссылкой-приглашением. */
    public static InviteLinkResponse createInviteLinkResponse() {
        return new InviteLinkResponse()
                .url("https://t.me/testbot/app?startapp=group_0123456789abcdef0123456789abcdef")
                .startParam("group_0123456789abcdef0123456789abcdef")
                .shareText("Присоединяйся к группе «Friends»");
    }
}
