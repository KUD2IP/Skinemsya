package skinemsya.vse.ru.receipts.api;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import skinemsya.vse.ru.common.domain.DomainException;
import skinemsya.vse.ru.common.domain.ErrorCode;
import skinemsya.vse.ru.common.security.AuthenticatedUser;
import skinemsya.vse.ru.receipts.api.dto.UpdateSelectionsRequest;
import skinemsya.vse.ru.receipts.application.SelectionService;

@RestController
public class SelectionController {

    private final SelectionService selectionService;

    public SelectionController(SelectionService selectionService) {
        this.selectionService = selectionService;
    }

    @PutMapping("/api/v1/events/{eventId}/selections")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateSelections(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable long eventId,
            @Valid @RequestBody UpdateSelectionsRequest request
    ) {
        long userId = requireUserId(authenticatedUser);
        var updates = request.selections().stream()
                .map(item -> new SelectionService.SelectionUpdate(item.positionId(), item.quantity()))
                .toList();
        selectionService.updateSelections(eventId, userId, updates);
    }

    @PostMapping("/api/v1/events/{eventId}/complete-selection")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void completeSelection(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable long eventId
    ) {
        selectionService.completeSelection(eventId, requireUserId(authenticatedUser));
    }

    private static long requireUserId(AuthenticatedUser authenticatedUser) {
        if (authenticatedUser == null) {
            throw new DomainException(ErrorCode.AUTHENTICATION_ERROR, "User is not authenticated");
        }
        return authenticatedUser.getUserId();
    }
}
