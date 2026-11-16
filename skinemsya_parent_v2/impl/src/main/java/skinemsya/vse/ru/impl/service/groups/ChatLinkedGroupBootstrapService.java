package skinemsya.vse.ru.impl.service.groups;

import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import skinemsya.vse.ru.api.internal.enums.ErrorCode;
import skinemsya.vse.ru.api.internal.enums.groups.GroupRole;
import skinemsya.vse.ru.api.internal.event.groups.GroupMemberJoined;
import skinemsya.vse.ru.api.internal.ex.DomainException;
import skinemsya.vse.ru.api.internal.pojo.groups.Group;
import skinemsya.vse.ru.api.internal.repository.groups.GroupMemberRepository;
import skinemsya.vse.ru.api.internal.repository.groups.GroupRepository;
import skinemsya.vse.ru.api.internal.service.users.UserService;
import skinemsya.vse.ru.impl.support.util.InviteCodes;

/** Создание и присоединение к группе, связанной с Telegram-чатом. */
@Service
@RequiredArgsConstructor
public class ChatLinkedGroupBootstrapService {

    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final UserService userService;
    private final ApplicationEventPublisher eventPublisher;

    /** Создаёт группу для чата, если её нет, и добавляет пользователя с корректной ролью. */
    @Transactional
    public Group joinOrCreateFromChat(long telegramChatId, String chatTitle, long userId) {
        requireUserExists(userId);

        var now = Instant.now();
        groupRepository.insertChatLinkedGroupIfAbsent(
                resolveChatTitle(chatTitle), telegramChatId, userId, InviteCodes.random(), now, now);

        var group = groupRepository
                .findByTelegramChatId(telegramChatId)
                .orElseThrow(() -> new IllegalStateException(
                        "Chat-linked group not found after upsert: chatId=" + telegramChatId));

        var role = group.getOwnerId() == userId ? GroupRole.OWNER : GroupRole.MEMBER;
        groupMemberRepository.insertIfAbsent(group.getId(), userId, role, now);

        eventPublisher.publishEvent(new GroupMemberJoined(group.getId(), userId));

        return group;
    }

    private void requireUserExists(long userId) {
        userService
                .findById(userId)
                .orElseThrow(() -> new DomainException(ErrorCode.NOT_FOUND, "Group user not found"));
    }

    private static String resolveChatTitle(String chatTitle) {
        if (chatTitle == null || chatTitle.isBlank()) {
            return "Telegram chat";
        }
        return chatTitle.trim();
    }
}
