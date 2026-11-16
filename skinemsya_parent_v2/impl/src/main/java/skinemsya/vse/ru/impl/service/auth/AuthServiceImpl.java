package skinemsya.vse.ru.impl.service.auth;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import skinemsya.vse.ru.api.internal.enums.auth.ChatSuggestedAction;
import skinemsya.vse.ru.api.internal.pojo.auth.AuthResult;
import skinemsya.vse.ru.api.internal.pojo.auth.ChatBootstrap;
import skinemsya.vse.ru.api.internal.pojo.auth.TokenPair;
import skinemsya.vse.ru.api.internal.service.auth.AuthEventService;
import skinemsya.vse.ru.api.internal.service.auth.AuthGroupService;
import skinemsya.vse.ru.api.internal.service.auth.AuthService;
import skinemsya.vse.ru.api.internal.service.auth.EventDeepLinkAccessService;
import skinemsya.vse.ru.api.internal.pojo.telegram.TelegramInitData;
import skinemsya.vse.ru.api.internal.service.telegram.TelegramChatService;
import skinemsya.vse.ru.api.internal.service.telegram.TelegramInitDataValidator;
import skinemsya.vse.ru.api.internal.pojo.users.TelegramUserData;
import skinemsya.vse.ru.api.internal.service.users.UserService;

/** Основной сценарий аутентификации через Telegram Mini App. */
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    private final TelegramInitDataValidator telegramInitDataValidator;
    private final UserService userService;
    private final JwtTokenService jwtTokenService;
    private final RefreshTokenService refreshTokenService;
    private final AuthGroupService authGroupService;
    private final TelegramChatService telegramChatService;
    private final AuthEventService authEventService;
    private final EventDeepLinkAccessService eventDeepLinkAccessService;

    /** {@inheritDoc} */
    @Override
    public AuthResult authenticate(String initData) {
        var validatedInitData = telegramInitDataValidator.validateWithChat(initData);
        var session = createSession(validatedInitData);
        validatedInitData.getChat().ifPresent(chat -> {
            try {
                var title = resolveChatTitle(chat.getChatId(), chat.getTitle());
                authGroupService.createFromChat(chat.getChatId(), title, session.userId());
            } catch (RuntimeException ex) {
                log.warn(
                        "Chat-linked group bootstrap failed for telegramChatId={}, userId={}",
                        chat.getChatId(),
                        session.userId(),
                        ex);
            }
        });
        Long eventId = validatedInitData
                .getEventInviteCode()
                .flatMap(authEventService::findIdByInviteCode)
                .orElse(null);
        Long groupId = validatedInitData
                .getGroupInviteCode()
                .flatMap(authGroupService::findIdByInviteCode)
                .orElse(null);
        if (eventId != null) {
            try {
                eventDeepLinkAccessService.ensureAccess(eventId, session.userId());
            } catch (RuntimeException ex) {
                log.warn(
                        "Event deep link access bootstrap failed for eventId={}, userId={}",
                        eventId,
                        session.userId(),
                        ex);
            }
        }
        if (groupId != null) {
            try {
                authGroupService.joinFromInvite(groupId, session.userId());
            } catch (RuntimeException ex) {
                log.warn("Group invite bootstrap failed for groupId={}, userId={}", groupId, session.userId(), ex);
            }
        }
        return new AuthResult(session.tokens(), buildBootstrap(validatedInitData, eventId, groupId));
    }

    private Optional<ChatBootstrap> buildBootstrap(TelegramInitData validatedInitData, Long eventId, Long groupId) {
        if (eventId != null) {
            return authEventService.findById(eventId).map(event -> {
                var group = authGroupService.findById(event.getGroupId()).orElse(null);
                return new ChatBootstrap(
                        event.getGroupId(),
                        group != null ? group.getName() : null,
                        group != null ? group.getType() : null,
                        ChatSuggestedAction.OPEN_APP,
                        eventId);
            });
        }
        if (groupId != null) {
            return authGroupService
                    .findById(groupId)
                    .map(group -> new ChatBootstrap(
                            group.getId(), group.getName(), group.getType(), ChatSuggestedAction.OPEN_APP, null));
        }
        return validatedInitData.getChat().flatMap(chat -> authGroupService
                .findByTelegramChatId(chat.getChatId())
                .map(group -> new ChatBootstrap(
                        group.getId(), group.getName(), group.getType(), ChatSuggestedAction.OPEN_APP, null)));
    }

    private String resolveChatTitle(long chatId, String titleFromInitData) {
        if (titleFromInitData != null && !titleFromInitData.isBlank()) {
            return titleFromInitData;
        }
        try {
            return telegramChatService.getChatTitle(chatId).orElse(null);
        } catch (RuntimeException ex) {
            log.warn("Could not resolve Telegram chat title for chatId={}", chatId, ex);
            return null;
        }
    }

    @Transactional
    AuthSession createSession(TelegramInitData validatedInitData) {
        var identity = validatedInitData.getIdentity();
        var user = userService.upsertFromTelegram(new TelegramUserData(
                identity.getTelegramUserId(),
                identity.getDisplayName(),
                identity.getTelegramUsername(),
                identity.getPhotoUrl()));
        refreshTokenService.revokeAllActiveForUser(user.getId());
        var tokens = issueTokenPair(user.getId());
        return new AuthSession(user.getId(), tokens);
    }

    /** {@inheritDoc} */
    @Override
    public TokenPair refresh(String refreshToken) {
        var rotated = refreshTokenService.rotate(refreshToken);
        var accessToken = jwtTokenService.createAccessToken(rotated.getUserId());
        return new TokenPair(accessToken, rotated.getRawToken(), jwtTokenService.accessTtlSeconds());
    }

    /** {@inheritDoc} */
    @Override
    public void revoke(String refreshToken) {
        refreshTokenService.revoke(refreshToken);
    }

    private TokenPair issueTokenPair(long userId) {
        var accessToken = jwtTokenService.createAccessToken(userId);
        var refresh = refreshTokenService.issue(userId);
        return new TokenPair(accessToken, refresh.getRawToken(), jwtTokenService.accessTtlSeconds());
    }

    private record AuthSession(long userId, TokenPair tokens) {}
}
