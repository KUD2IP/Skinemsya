package skinemsya.vse.ru.impl.rest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import skinemsya.vse.ru.api.internal.enums.ErrorCode;
import skinemsya.vse.ru.api.internal.ex.DomainException;
import skinemsya.vse.ru.api.internal.pojo.common.PageRequest;
import skinemsya.vse.ru.api.internal.pojo.common.PageResult;
import skinemsya.vse.ru.api.internal.pojo.events.Event;
import skinemsya.vse.ru.api.internal.pojo.users.PayoutRequisites;
import skinemsya.vse.ru.api.internal.security.AuthenticatedUser;
import skinemsya.vse.ru.api.internal.service.events.EventAccessPort;
import skinemsya.vse.ru.api.internal.service.events.EventService;
import skinemsya.vse.ru.api.internal.service.groups.GroupAccessService;
import skinemsya.vse.ru.api.internal.service.groups.GroupService;
import skinemsya.vse.ru.api.internal.service.telegram.TelegramBotClient;
import skinemsya.vse.ru.api.internal.service.users.UserService;
import skinemsya.vse.ru.api.rest.events.dto.CreateEventRequest;
import skinemsya.vse.ru.api.rest.events.dto.EventResponse;
import skinemsya.vse.ru.api.rest.events.dto.InviteLinkResponse;
import skinemsya.vse.ru.api.rest.events.dto.PageEventResponse;
import skinemsya.vse.ru.api.rest.events.dto.UpdateEventRequest;
import skinemsya.vse.ru.api.rest.events.dto.UpdateExpectedParticipantsRequest;
import skinemsya.vse.ru.impl.mapper.rest.EventRestMapper;
import skinemsya.vse.ru.impl.support.util.InviteShareText;
import skinemsya.vse.ru.impl.support.util.TelegramStartParam;

/** REST API сборов. */
@RestController
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;
    private final EventAccessPort eventAccessPort;
    private final GroupAccessService groupAccessService;
    private final GroupService groupService;
    private final TelegramBotClient telegramBotClient;
    private final UserService userService;
    private final EventRestMapper eventRestMapper;

    /** Создаёт сбор в группе. */
    @PostMapping("/api/v1/groups/{groupId}/events")
    @ResponseStatus(HttpStatus.CREATED)
    public EventResponse createEvent(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable long groupId,
            @Valid @RequestBody CreateEventRequest request) {
        long userId = requireUserId(authenticatedUser);
        return toResponse(
                eventService.create(
                        groupId,
                        request.getName(),
                        request.getDescription(),
                        request.getPayerId(),
                        userId,
                        request.getExpectedParticipantCount()),
                userId);
    }

    /** Возвращает страницу сборов группы. */
    @GetMapping("/api/v1/groups/{groupId}/events")
    public PageEventResponse listEvents(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable long groupId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        long userId = requireUserId(authenticatedUser);
        var result = eventService.listByGroup(groupId, userId, resolvePageRequest(page, size));
        var items = result.items().stream().map(event -> toResponse(event, userId)).toList();
        return eventRestMapper.toEventPageResponse(
                new PageResult<>(items, result.page(), result.size(), result.totalElements()));
    }

    /** Возвращает ссылку-приглашение в сбор. */
    @GetMapping("/api/v1/events/{eventId}/invite-link")
    public InviteLinkResponse getInviteLink(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser, @PathVariable long eventId) {
        long userId = requireUserId(authenticatedUser);
        var event = eventService
                .findById(eventId)
                .orElseThrow(() -> new DomainException(ErrorCode.NOT_FOUND, "Event not found"));
        groupAccessService.requireMember(event.getGroupId(), userId);
        var group = groupService
                .findById(event.getGroupId())
                .orElseThrow(() -> new DomainException(ErrorCode.NOT_FOUND, "Group not found"));
        String startParam = TelegramStartParam.forEventCode(eventService.inviteCode(eventId));
        return new InviteLinkResponse()
                .url(telegramBotClient.buildMiniAppDeepLink(startParam))
                .startParam(startParam)
                .shareText(InviteShareText.forEvent(event.getName(), group.getName()));
    }

    /** Возвращает сбор по идентификатору. */
    @GetMapping("/api/v1/events/{eventId}")
    public EventResponse getEvent(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser, @PathVariable long eventId) {
        long userId = requireUserId(authenticatedUser);
        var event = eventService
                .findById(eventId)
                .orElseThrow(() -> new DomainException(ErrorCode.NOT_FOUND, "Event not found"));
        groupAccessService.requireMember(event.getGroupId(), userId);
        return toResponse(event, userId);
    }

    /** Обновляет черновик сбора. */
    @PutMapping("/api/v1/events/{eventId}")
    public EventResponse updateEvent(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable long eventId,
            @Valid @RequestBody UpdateEventRequest request) {
        long userId = requireUserId(authenticatedUser);
        return toResponse(
                eventService.update(
                        eventId,
                        userId,
                        request.getName(),
                        request.getDescription(),
                        request.getPayerId(),
                        request.getExpectedParticipantCount()),
                userId);
    }

    /** Помечает сбор удалённым. */
    @DeleteMapping("/api/v1/events/{eventId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteEvent(@AuthenticationPrincipal AuthenticatedUser authenticatedUser, @PathVariable long eventId) {
        long userId = requireUserId(authenticatedUser);
        eventService.delete(eventId, userId);
    }

    /** Отправляет черновик сбора на распределение. */
    @PostMapping("/api/v1/events/{eventId}/send-to-distribution")
    public EventResponse sendToDistribution(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser, @PathVariable long eventId) {
        long userId = requireUserId(authenticatedUser);
        return toResponse(eventAccessPort.sendToDistribution(eventId, userId), userId);
    }

    /** Возвращает сбор в черновик. */
    @PostMapping("/api/v1/events/{eventId}/revert-to-draft")
    public EventResponse revertToDraft(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser, @PathVariable long eventId) {
        long userId = requireUserId(authenticatedUser);
        return toResponse(eventService.revertToDraft(eventId, userId), userId);
    }

    /** Закрывает рассчитанный сбор. */
    @PostMapping("/api/v1/events/{eventId}/close")
    public EventResponse closeEvent(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser, @PathVariable long eventId) {
        long userId = requireUserId(authenticatedUser);
        return toResponse(eventAccessPort.closeByPayer(eventId, userId), userId);
    }

    /** Добавляет текущего пользователя в сбор. */
    @PostMapping("/api/v1/events/{eventId}/join")
    public EventResponse joinEvent(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser, @PathVariable long eventId) {
        long userId = requireUserId(authenticatedUser);
        return toResponse(eventService.join(eventId, userId), userId);
    }

    /** Выводит текущего пользователя из сбора. */
    @PostMapping("/api/v1/events/{eventId}/leave")
    public EventResponse leaveEvent(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser, @PathVariable long eventId) {
        long userId = requireUserId(authenticatedUser);
        return toResponse(eventService.leave(eventId, userId), userId);
    }

    /** Удаляет участника из сбора. */
    @DeleteMapping("/api/v1/events/{eventId}/participants/{targetUserId}")
    public EventResponse removeParticipant(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable long eventId,
            @PathVariable long targetUserId) {
        long userId = requireUserId(authenticatedUser);
        return toResponse(eventService.removeParticipant(eventId, userId, targetUserId), userId);
    }

    /** Меняет ожидаемое число участников сбора. */
    @PutMapping("/api/v1/events/{eventId}/expected-participants")
    public EventResponse updateExpectedParticipants(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable long eventId,
            @Valid @RequestBody UpdateExpectedParticipantsRequest request) {
        long userId = requireUserId(authenticatedUser);
        return toResponse(
                eventService.updateExpectedParticipantCount(eventId, userId, request.getExpectedParticipantCount()),
                userId);
    }

    private EventResponse toResponse(Event event, long userId) {
        return eventRestMapper.toResponse(
                event,
                PayoutRequisites.hasAny(userService.getPaymentDetails(event.getPayerId())),
                eventAccessPort.countParticipants(event.getId()),
                eventAccessPort.isParticipant(event.getId(), userId));
    }

    private static long requireUserId(AuthenticatedUser authenticatedUser) {
        if (authenticatedUser == null) {
            throw new DomainException(ErrorCode.AUTHENTICATION_ERROR, "User is not authenticated");
        }
        return authenticatedUser.getUserId();
    }

    private static PageRequest resolvePageRequest(Integer page, Integer size) {
        if (page == null && size == null) {
            return PageRequest.defaults();
        }
        return PageRequest.of(page == null ? 0 : page, size == null ? PageRequest.defaults().size() : size);
    }
}
