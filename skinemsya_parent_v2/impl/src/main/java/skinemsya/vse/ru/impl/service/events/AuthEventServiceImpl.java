package skinemsya.vse.ru.impl.service.events;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import skinemsya.vse.ru.api.internal.pojo.auth.AuthEvent;
import skinemsya.vse.ru.api.internal.service.auth.AuthEventService;
import skinemsya.vse.ru.api.internal.service.events.EventService;

/** Операции со сборами, нужные при аутентификации. */
@Service
@RequiredArgsConstructor
public class AuthEventServiceImpl implements AuthEventService {

    private final EventService eventService;

    @Override
    public Optional<Long> findIdByInviteCode(String inviteCode) {
        return eventService.findIdByInviteCode(inviteCode);
    }

    @Override
    public Optional<AuthEvent> findById(long eventId) {
        return eventService.findById(eventId).map(event -> new AuthEvent(event.getId(), event.getGroupId()));
    }
}
