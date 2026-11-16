package skinemsya.vse.ru.api.internal.service.auth;

import java.util.Optional;
import skinemsya.vse.ru.api.internal.pojo.auth.AuthEvent;

/** Операции с событиями, необходимые при аутентификации. */
public interface AuthEventService {

    /** Находит идентификатор события по коду приглашения. */
    Optional<Long> findIdByInviteCode(String inviteCode);

    /** Находит событие по идентификатору. */
    Optional<AuthEvent> findById(long eventId);
}
