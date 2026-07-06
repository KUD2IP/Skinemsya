package skinemsya.vse.ru.events.api;

import jakarta.validation.Valid;
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
import skinemsya.vse.ru.common.api.PageRequest;
import skinemsya.vse.ru.common.api.PageResult;
import skinemsya.vse.ru.common.domain.DomainException;
import skinemsya.vse.ru.common.domain.ErrorCode;
import skinemsya.vse.ru.common.security.AuthenticatedUser;
import skinemsya.vse.ru.events.api.dto.CreateEventRequest;
import skinemsya.vse.ru.events.api.dto.EventResponse;
import skinemsya.vse.ru.events.api.dto.UpdateEventRequest;
import skinemsya.vse.ru.events.application.EventAccessPort;
import skinemsya.vse.ru.events.application.EventService;
import skinemsya.vse.ru.events.domain.Event;
import skinemsya.vse.ru.events.domain.exception.EventNotFoundException;
import skinemsya.vse.ru.groups.application.GroupAccessService;
import skinemsya.vse.ru.users.application.UserService;
import skinemsya.vse.ru.users.domain.PayoutRequisites;

@RestController
public class EventController {

    private final EventService eventService;
    private final EventAccessPort eventAccessPort;
    private final GroupAccessService groupAccessService;
    private final UserService userService;

    public EventController(
            EventService eventService,
            EventAccessPort eventAccessPort,
            GroupAccessService groupAccessService,
            UserService userService) {
        this.eventService = eventService;
        this.eventAccessPort = eventAccessPort;
        this.groupAccessService = groupAccessService;
        this.userService = userService;
    }

    @PostMapping("/api/v1/groups/{groupId}/events")
    @ResponseStatus(HttpStatus.CREATED)
    public EventResponse createEvent(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable long groupId,
            @Valid @RequestBody CreateEventRequest request) {
        long userId = requireUserId(authenticatedUser);
        return toResponse(
                eventService.create(groupId, request.name(), request.description(), request.payerId(), userId));
    }

    @GetMapping("/api/v1/groups/{groupId}/events")
    public PageResult<EventResponse> listEvents(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable long groupId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        long userId = requireUserId(authenticatedUser);
        var result = eventService.listByGroup(groupId, userId, resolvePageRequest(page, size));
        return mapPage(result, this::toResponse);
    }

    @GetMapping("/api/v1/events/{eventId}")
    public EventResponse getEvent(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser, @PathVariable long eventId) {
        long userId = requireUserId(authenticatedUser);
        var event = eventService.findById(eventId).orElseThrow(EventNotFoundException::new);
        groupAccessService.requireMember(event.groupId(), userId);
        return toResponse(event);
    }

    @PutMapping("/api/v1/events/{eventId}")
    public EventResponse updateEvent(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable long eventId,
            @Valid @RequestBody UpdateEventRequest request) {
        long userId = requireUserId(authenticatedUser);
        return toResponse(
                eventService.update(eventId, userId, request.name(), request.description(), request.payerId()));
    }

    @DeleteMapping("/api/v1/events/{eventId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteEvent(@AuthenticationPrincipal AuthenticatedUser authenticatedUser, @PathVariable long eventId) {
        long userId = requireUserId(authenticatedUser);
        eventService.delete(eventId, userId);
    }

    @PostMapping("/api/v1/events/{eventId}/send-to-distribution")
    public EventResponse sendToDistribution(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser, @PathVariable long eventId) {
        long userId = requireUserId(authenticatedUser);
        return toResponse(eventAccessPort.sendToDistribution(eventId, userId));
    }

    @PostMapping("/api/v1/events/{eventId}/close")
    public EventResponse closeEvent(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser, @PathVariable long eventId) {
        long userId = requireUserId(authenticatedUser);
        return toResponse(eventAccessPort.closeByPayer(eventId, userId));
    }

    private static long requireUserId(AuthenticatedUser authenticatedUser) {
        if (authenticatedUser == null) {
            throw new DomainException(ErrorCode.AUTHENTICATION_ERROR, "User is not authenticated");
        }
        return authenticatedUser.getUserId();
    }

    private EventResponse toResponse(Event event) {
        return new EventResponse(
                event.id(),
                event.groupId(),
                event.name(),
                event.description(),
                event.payerId(),
                event.createdBy(),
                event.status(),
                PayoutRequisites.hasAny(userService.getPaymentDetails(event.payerId())),
                event.createdAt(),
                event.updatedAt());
    }

    private static PageRequest resolvePageRequest(Integer page, Integer size) {
        if (page == null && size == null) {
            return PageRequest.defaults();
        }
        return PageRequest.of(
                page == null ? 0 : page, size == null ? PageRequest.defaults().size() : size);
    }

    private static <T, R> PageResult<R> mapPage(PageResult<T> source, java.util.function.Function<T, R> mapper) {
        return new PageResult<>(
                source.items().stream().map(mapper).toList(), source.page(), source.size(), source.totalElements());
    }
}
