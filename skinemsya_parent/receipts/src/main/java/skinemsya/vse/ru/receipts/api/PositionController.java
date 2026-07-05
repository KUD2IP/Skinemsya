package skinemsya.vse.ru.receipts.api;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import skinemsya.vse.ru.common.domain.DomainException;
import skinemsya.vse.ru.common.domain.ErrorCode;
import skinemsya.vse.ru.common.security.AuthenticatedUser;
import skinemsya.vse.ru.events.application.EventAccessPort;
import skinemsya.vse.ru.events.domain.EventStatus;
import skinemsya.vse.ru.receipts.api.dto.CreatePositionRequest;
import skinemsya.vse.ru.receipts.api.dto.MarkSharedRequest;
import skinemsya.vse.ru.receipts.api.dto.PositionResponse;
import skinemsya.vse.ru.receipts.api.dto.UpdatePositionRequest;
import skinemsya.vse.ru.receipts.application.PositionAvailabilityService;
import skinemsya.vse.ru.receipts.application.PositionService;
import skinemsya.vse.ru.receipts.infrastructure.persistence.PositionRepository;

@RestController
public class PositionController {

    private final PositionService positionService;
    private final PositionRepository positionRepository;
    private final PositionAvailabilityService positionAvailabilityService;
    private final EventAccessPort eventAccessPort;

    public PositionController(
            PositionService positionService,
            PositionRepository positionRepository,
            PositionAvailabilityService positionAvailabilityService,
            EventAccessPort eventAccessPort) {
        this.positionService = positionService;
        this.positionRepository = positionRepository;
        this.positionAvailabilityService = positionAvailabilityService;
        this.eventAccessPort = eventAccessPort;
    }

    @GetMapping("/api/v1/events/{eventId}/positions")
    public List<PositionResponse> listPositions(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser, @PathVariable long eventId) {
        long userId = requireUserId(authenticatedUser);
        var status = eventAccessPort.getStatus(eventId);
        return positionService.listByEvent(eventId, userId).stream()
                .map(position -> {
                    if (status != EventStatus.DISTRIBUTION || position.shared()) {
                        return PositionResponse.from(position);
                    }
                    var entity = positionRepository.findById(position.id()).orElseThrow();
                    var availability = positionAvailabilityService.availabilityFor(entity, userId);
                    return PositionResponse.from(position, availability);
                })
                .toList();
    }

    @PostMapping("/api/v1/events/{eventId}/positions")
    @ResponseStatus(HttpStatus.CREATED)
    public PositionResponse addPosition(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable long eventId,
            @Valid @RequestBody CreatePositionRequest request) {
        long userId = requireUserId(authenticatedUser);
        return PositionResponse.from(positionService.addManual(
                eventId, userId, request.name(), request.quantity(), request.totalPriceKopecks()));
    }

    @PutMapping("/api/v1/positions/{id}")
    public PositionResponse updatePosition(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable long id,
            @Valid @RequestBody UpdatePositionRequest request) {
        long userId = requireUserId(authenticatedUser);
        return PositionResponse.from(
                positionService.update(id, userId, request.name(), request.quantity(), request.totalPriceKopecks()));
    }

    @DeleteMapping("/api/v1/positions/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePosition(@AuthenticationPrincipal AuthenticatedUser authenticatedUser, @PathVariable long id) {
        positionService.delete(id, requireUserId(authenticatedUser));
    }

    @PostMapping("/api/v1/positions/{id}/mark-shared")
    public PositionResponse markShared(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable long id,
            @Valid @RequestBody MarkSharedRequest request) {
        long userId = requireUserId(authenticatedUser);
        return PositionResponse.from(positionService.markShared(id, userId, request.forAll(), List.of()));
    }

    private static long requireUserId(AuthenticatedUser authenticatedUser) {
        if (authenticatedUser == null) {
            throw new DomainException(ErrorCode.AUTHENTICATION_ERROR, "User is not authenticated");
        }
        return authenticatedUser.getUserId();
    }
}
