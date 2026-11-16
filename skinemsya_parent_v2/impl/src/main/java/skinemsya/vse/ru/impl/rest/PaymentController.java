package skinemsya.vse.ru.impl.rest;

import java.util.List;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import skinemsya.vse.ru.api.internal.enums.ErrorCode;
import skinemsya.vse.ru.api.internal.ex.DomainException;
import skinemsya.vse.ru.api.internal.security.AuthenticatedUser;
import skinemsya.vse.ru.api.internal.service.payments.PaymentService;
import skinemsya.vse.ru.api.rest.payments.dto.ConfirmDebtorRequest;
import skinemsya.vse.ru.api.rest.payments.dto.PaymentDetailsResponse;
import skinemsya.vse.ru.api.rest.payments.dto.PaymentResponse;
import skinemsya.vse.ru.impl.mapper.rest.PaymentRestMapper;

/** REST API платежей по долгам. */
@RestController
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final PaymentRestMapper paymentRestMapper;

    /** Возвращает реквизиты для оплаты долга. */
    @GetMapping("/api/v1/debts/{debtId}/payment-details")
    public PaymentDetailsResponse getPaymentDetails(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser, @PathVariable long debtId) {
        return paymentRestMapper.toResponse(
                paymentService.getPaymentDetails(debtId, requireUserId(authenticatedUser)));
    }

    /** Должник отмечает перевод отправленным. */
    @PostMapping("/api/v1/debts/{debtId}/payment/confirm-debtor")
    public PaymentResponse confirmDebtor(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable long debtId,
            @Valid @RequestBody ConfirmDebtorRequest request) {
        return paymentRestMapper.toResponse(paymentService.confirmByDebtor(
                debtId, requireUserId(authenticatedUser), request.getScreenshotFileId()));
    }

    /** Плательщик подтверждает все отмеченные переводы сбора. */
    @PostMapping("/api/v1/events/{eventId}/payments/confirm-all")
    public List<PaymentResponse> confirmAll(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser, @PathVariable long eventId) {
        return paymentService.confirmAllForEvent(eventId, requireUserId(authenticatedUser)).stream()
                .map(paymentRestMapper::toResponse)
                .toList();
    }

    /** Плательщик подтверждает перевод. */
    @PostMapping("/api/v1/payments/{id}/confirm-payer")
    public PaymentResponse confirmPayer(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser, @PathVariable long id) {
        return paymentRestMapper.toResponse(paymentService.confirmByPayer(id, requireUserId(authenticatedUser)));
    }

    /** Плательщик подтверждает перевод по долгу. */
    @PostMapping("/api/v1/debts/{debtId}/payment/confirm-payer")
    public PaymentResponse confirmPayerByDebt(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser, @PathVariable long debtId) {
        return paymentRestMapper.toResponse(
                paymentService.confirmByPayerForDebt(debtId, requireUserId(authenticatedUser)));
    }

    /** Плательщик оспаривает перевод. */
    @PostMapping("/api/v1/payments/{id}/dispute")
    public PaymentResponse dispute(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser, @PathVariable long id) {
        return paymentRestMapper.toResponse(paymentService.dispute(id, requireUserId(authenticatedUser)));
    }

    /** Плательщик оспаривает перевод по долгу. */
    @PostMapping("/api/v1/debts/{debtId}/payment/dispute")
    public PaymentResponse disputeByDebt(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser, @PathVariable long debtId) {
        return paymentRestMapper.toResponse(
                paymentService.disputeForDebt(debtId, requireUserId(authenticatedUser)));
    }

    private static long requireUserId(AuthenticatedUser authenticatedUser) {
        if (authenticatedUser == null) {
            throw new DomainException(ErrorCode.AUTHENTICATION_ERROR, "User is not authenticated");
        }
        return authenticatedUser.getUserId();
    }
}
