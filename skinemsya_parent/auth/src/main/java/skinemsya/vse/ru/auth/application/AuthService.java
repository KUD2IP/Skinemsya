package skinemsya.vse.ru.auth.application;

import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import skinemsya.vse.ru.auth.domain.AuthResult;
import skinemsya.vse.ru.auth.domain.ChatBootstrap;
import skinemsya.vse.ru.auth.domain.ChatSuggestedAction;
import skinemsya.vse.ru.auth.domain.TokenPair;
import skinemsya.vse.ru.events.application.EventDeepLinkAccessService;
import skinemsya.vse.ru.events.application.EventService;
import skinemsya.vse.ru.groups.application.GroupService;
import skinemsya.vse.ru.integrations.application.TelegramBotClient;
import skinemsya.vse.ru.integrations.application.TelegramInitDataValidator;
import skinemsya.vse.ru.integrations.domain.TelegramInitData;
import skinemsya.vse.ru.users.application.UserService;
import skinemsya.vse.ru.users.domain.TelegramUserData;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final TelegramInitDataValidator telegramInitDataValidator;
    private final UserService userService;
    private final JwtTokenService jwtTokenService;
    private final RefreshTokenService refreshTokenService;
    private final GroupService groupService;
    private final TelegramBotClient telegramBotClient;
    private final EventService eventService;
    private final EventDeepLinkAccessService eventDeepLinkAccessService;

    public AuthService(
            TelegramInitDataValidator telegramInitDataValidator,
            UserService userService,
            JwtTokenService jwtTokenService,
            RefreshTokenService refreshTokenService,
            GroupService groupService,
            TelegramBotClient telegramBotClient,
            EventService eventService,
            EventDeepLinkAccessService eventDeepLinkAccessService) {
        this.telegramInitDataValidator = telegramInitDataValidator;
        this.userService = userService;
        this.jwtTokenService = jwtTokenService;
        this.refreshTokenService = refreshTokenService;
        this.groupService = groupService;
        this.telegramBotClient = telegramBotClient;
        this.eventService = eventService;
        this.eventDeepLinkAccessService = eventDeepLinkAccessService;
    }

    public AuthResult authenticate(String initData) {
        var validatedInitData = telegramInitDataValidator.validateWithChat(initData);
        var session = createSession(validatedInitData);
        validatedInitData.chat().ifPresent(chat -> {
            try {
                var title = resolveChatTitle(chat.chatId(), chat.title());
                groupService.createFromChat(chat.chatId(), title, session.userId());
            } catch (RuntimeException ex) {
                log.warn(
                        "Chat-linked group bootstrap failed for telegramChatId={}, userId={}",
                        chat.chatId(),
                        session.userId(),
                        ex);
            }
        });
        validatedInitData.eventId().ifPresent(eventId -> {
            try {
                eventDeepLinkAccessService.ensureAccess(eventId, session.userId());
            } catch (RuntimeException ex) {
                log.warn(
                        "Event deep link access bootstrap failed for eventId={}, userId={}",
                        eventId,
                        session.userId(),
                        ex);
            }
        });
        return new AuthResult(session.tokens(), buildBootstrap(validatedInitData, session.userId()));
    }

    private Optional<ChatBootstrap> buildBootstrap(TelegramInitData validatedInitData, long userId) {
        if (validatedInitData.eventId().isPresent()) {
            long eventId = validatedInitData.eventId().get();
            return eventService.findById(eventId).map(event -> {
                var group = groupService.findById(event.groupId()).orElse(null);
                return new ChatBootstrap(
                        event.groupId(),
                        group != null ? group.name() : null,
                        group != null ? group.type() : null,
                        ChatSuggestedAction.OPEN_APP,
                        eventId);
            });
        }
        return validatedInitData.chat().flatMap(chat -> groupService
                .findByTelegramChatId(chat.chatId())
                .map(group ->
                        new ChatBootstrap(group.id(), group.name(), group.type(), ChatSuggestedAction.OPEN_APP, null)));
    }

    private String resolveChatTitle(long chatId, String titleFromInitData) {
        if (titleFromInitData != null && !titleFromInitData.isBlank()) {
            return titleFromInitData;
        }
        try {
            return telegramBotClient.getChatTitle(chatId).orElse(null);
        } catch (RuntimeException ex) {
            log.warn("Could not resolve Telegram chat title for chatId={}", chatId, ex);
            return null;
        }
    }

    @Transactional
    AuthSession createSession(TelegramInitData validatedInitData) {
        var user = userService.upsertFromTelegram(new TelegramUserData(
                validatedInitData.identity().telegramUserId(),
                validatedInitData.identity().displayName(),
                validatedInitData.identity().telegramUsername()));
        refreshTokenService.revokeAllActiveForUser(user.id());
        var tokens = issueTokenPair(user.id());
        return new AuthSession(user.id(), tokens);
    }

    private record AuthSession(long userId, TokenPair tokens) {}

    public TokenPair refresh(String refreshToken) {
        var rotated = refreshTokenService.rotate(refreshToken);
        var accessToken = jwtTokenService.createAccessToken(rotated.userId());
        return new TokenPair(accessToken, rotated.rawToken(), jwtTokenService.accessTtlSeconds());
    }

    public void revoke(String refreshToken) {
        refreshTokenService.revoke(refreshToken);
    }

    private TokenPair issueTokenPair(long userId) {
        var accessToken = jwtTokenService.createAccessToken(userId);
        var refresh = refreshTokenService.issue(userId);
        return new TokenPair(accessToken, refresh.rawToken(), jwtTokenService.accessTtlSeconds());
    }
}
