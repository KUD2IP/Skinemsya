package skinemsya.vse.ru.groups.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import skinemsya.vse.ru.groups.domain.Group;
import skinemsya.vse.ru.groups.domain.GroupRole;
import skinemsya.vse.ru.groups.domain.exception.GroupUserNotFoundException;
import skinemsya.vse.ru.groups.infrastructure.mapper.GroupMapper;
import skinemsya.vse.ru.groups.infrastructure.persistence.GroupEntity;
import skinemsya.vse.ru.groups.infrastructure.persistence.GroupMemberRepository;
import skinemsya.vse.ru.groups.infrastructure.persistence.GroupRepository;
import skinemsya.vse.ru.users.application.UserService;

import java.time.Instant;

@Service
public class ChatLinkedGroupBootstrapService {

    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final GroupMapper groupMapper;
    private final UserService userService;

    public ChatLinkedGroupBootstrapService(
            GroupRepository groupRepository,
            GroupMemberRepository groupMemberRepository,
            GroupMapper groupMapper,
            UserService userService
    ) {
        this.groupRepository = groupRepository;
        this.groupMemberRepository = groupMemberRepository;
        this.groupMapper = groupMapper;
        this.userService = userService;
    }

    @Transactional
    public Group joinOrCreateFromChat(long telegramChatId, String chatTitle, long userId) {
        requireUserExists(userId);

        var now = Instant.now();
        groupRepository.insertChatLinkedGroupIfAbsent(
                resolveChatTitle(chatTitle),
                telegramChatId,
                userId,
                now,
                now
        );

        var group = groupRepository.findByTelegramChatId(telegramChatId)
                .orElseThrow(() -> new IllegalStateException(
                        "Chat-linked group not found after upsert: chatId=" + telegramChatId
                ));

        var role = resolveRole(group, userId);
        groupMemberRepository.insertIfAbsent(group.getId(), userId, role.name(), now);

        return groupMapper.toDomain(group);
    }

    private static GroupRole resolveRole(GroupEntity group, long userId) {
        return group.getOwnerId() == userId ? GroupRole.OWNER : GroupRole.MEMBER;
    }

    private void requireUserExists(long userId) {
        userService.findById(userId)
                .orElseThrow(GroupUserNotFoundException::new);
    }

    private static String resolveChatTitle(String chatTitle) {
        if (chatTitle == null || chatTitle.isBlank()) {
            return "Telegram chat";
        }
        return chatTitle.trim();
    }
}
