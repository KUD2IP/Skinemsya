package skinemsya.vse.ru.impl.service.events;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import skinemsya.vse.ru.api.internal.enums.ErrorCode;
import skinemsya.vse.ru.api.internal.ex.DomainException;
import skinemsya.vse.ru.api.internal.service.auth.EventDeepLinkAccessService;
import skinemsya.vse.ru.api.internal.service.events.EventService;
import skinemsya.vse.ru.api.internal.service.groups.GroupAccessService;
import skinemsya.vse.ru.api.internal.service.groups.GroupService;

/** Выдаёт доступ к сбору по прямой ссылке: в группу и на свободное место. */
@Service
@Transactional
@RequiredArgsConstructor
public class EventDeepLinkAccessServiceImpl implements EventDeepLinkAccessService {

    private final EventService eventService;
    private final GroupService groupService;
    private final GroupAccessService groupAccessService;

    @Override
    public void ensureAccess(long eventId, long userId) {
        var event = eventService.findById(eventId).orElseThrow(() -> new DomainException(ErrorCode.NOT_FOUND, "Event not found"));
        var group = groupService
                .findById(event.getGroupId())
                .orElseThrow(() -> new DomainException(ErrorCode.NOT_FOUND, "Group not found"));

        if (!groupAccessService.isMember(group.getId(), userId)) {
            groupService.joinFromInvite(group.getId(), userId);
        }

        if (groupAccessService.isMember(group.getId(), userId)) {
            try {
                eventService.join(eventId, userId);
            } catch (DomainException ex) {
                if (!isOptionalJoinRejection(ex)) {
                    throw ex;
                }
            }
        }
    }

    private static boolean isOptionalJoinRejection(DomainException ex) {
        if (ex.errorCode() != ErrorCode.DOMAIN_CONFLICT) {
            return false;
        }
        String message = ex.getMessage();
        return "Event has no free seats".equals(message) || "Cannot join this event".equals(message);
    }
}
