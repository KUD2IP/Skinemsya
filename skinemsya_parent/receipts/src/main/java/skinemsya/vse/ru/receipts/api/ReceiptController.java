package skinemsya.vse.ru.receipts.api;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import skinemsya.vse.ru.common.domain.DomainException;
import skinemsya.vse.ru.common.domain.ErrorCode;
import skinemsya.vse.ru.common.security.AuthenticatedUser;
import skinemsya.vse.ru.receipts.api.dto.ProcessReceiptRequest;
import skinemsya.vse.ru.receipts.api.dto.ReceiptResponse;
import skinemsya.vse.ru.receipts.application.ReceiptService;

@RestController
public class ReceiptController {

    private final ReceiptService receiptService;

    public ReceiptController(ReceiptService receiptService) {
        this.receiptService = receiptService;
    }

    @PostMapping("/api/v1/events/{eventId}/receipts")
    @ResponseStatus(HttpStatus.CREATED)
    public ReceiptResponse processReceipt(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable long eventId,
            @Valid @RequestBody ProcessReceiptRequest request
    ) {
        long userId = requireUserId(authenticatedUser);
        return ReceiptResponse.from(receiptService.processReceipt(eventId, request.fileId(), userId));
    }

    @GetMapping("/api/v1/events/{eventId}/receipts")
    public java.util.List<ReceiptResponse> listReceipts(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable long eventId
    ) {
        long userId = requireUserId(authenticatedUser);
        return receiptService.listByEvent(eventId, userId).stream()
                .map(ReceiptResponse::from)
                .toList();
    }

    @PostMapping("/api/v1/events/{eventId}/receipts/{id}/split-tips")
    public ReceiptResponse splitTips(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable long eventId,
            @PathVariable long id
    ) {
        long userId = requireUserId(authenticatedUser);
        return ReceiptResponse.from(receiptService.splitTips(eventId, id, userId));
    }

    private static long requireUserId(AuthenticatedUser authenticatedUser) {
        if (authenticatedUser == null) {
            throw new DomainException(ErrorCode.AUTHENTICATION_ERROR, "User is not authenticated");
        }
        return authenticatedUser.getUserId();
    }
}
