package skinemsya.vse.ru.api.internal.service.auth;

import java.util.Optional;
import skinemsya.vse.ru.api.internal.pojo.auth.AuthGroup;

/** Операции с группами, необходимые при аутентификации. */
public interface AuthGroupService {

    /** Создаёт группу, связанную с Telegram-чатом. */
    void createFromChat(long chatId, String title, long userId);

    /** Находит идентификатор группы по коду приглашения. */
    Optional<Long> findIdByInviteCode(String inviteCode);

    /** Добавляет пользователя в группу по приглашению. */
    void joinFromInvite(long groupId, long userId);

    /** Находит группу по идентификатору. */
    Optional<AuthGroup> findById(long groupId);

    /** Находит группу по идентификатору Telegram-чата. */
    Optional<AuthGroup> findByTelegramChatId(long chatId);
}
