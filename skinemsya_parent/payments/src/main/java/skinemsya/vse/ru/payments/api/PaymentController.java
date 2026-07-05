package skinemsya.vse.ru.payments.api;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import skinemsya.vse.ru.common.domain.DomainException;
import skinemsya.vse.ru.common.domain.ErrorCode;
import skinemsya.vse.ru.common.security.AuthenticatedUser;
import skinemsya.vse.ru.payments.api.dto.ConfirmDebtorRequest;
import skinemsya.vse.ru.payments.api.dto.PaymentDetailsResponse;
import skinemsya.vse.ru.payments.api.dto.PaymentResponse;
import skinemsya.vse.ru.payments.application.PaymentService;

@RestController
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping("/api/v1/debts/{debtId}/payment-details")
    public PaymentDetailsResponse getPaymentDetails(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser, @PathVariable long debtId) {
        return PaymentDetailsResponse.from(paymentService.getPaymentDetails(debtId, requireUserId(authenticatedUser)));
    }

    @PostMapping("/api/v1/debts/{debtId}/payment/confirm-debtor")
    public PaymentResponse confirmDebtor(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable long debtId,
            @Valid @RequestBody ConfirmDebtorRequest request) {
        return PaymentResponse.from(
                paymentService.confirmByDebtor(debtId, requireUserId(authenticatedUser), request.screenshotFileId()));
    }

    @PostMapping("/api/v1/events/{eventId}/payments/confirm-all")
    public List<PaymentResponse> confirmAll(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser, @PathVariable long eventId) {
        return paymentService.confirmAllForEvent(eventId, requireUserId(authenticatedUser)).stream()
                .map(PaymentResponse::from)
                .toList();
    }

    @PostMapping("/api/v1/payments/{id}/confirm-payer")
    public PaymentResponse confirmPayer(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser, @PathVariable long id) {
        return PaymentResponse.from(paymentService.confirmByPayer(id, requireUserId(authenticatedUser)));
    }

    @PostMapping("/api/v1/debts/{debtId}/payment/confirm-payer")
    public PaymentResponse confirmPayerByDebt(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser, @PathVariable long debtId) {
        return PaymentResponse.from(paymentService.confirmByPayerForDebt(debtId, requireUserId(authenticatedUser)));
    }

    @PostMapping("/api/v1/payments/{id}/dispute")
    public PaymentResponse dispute(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser, @PathVariable long id) {
        return PaymentResponse.from(paymentService.dispute(id, requireUserId(authenticatedUser)));
    }

    @PostMapping("/api/v1/debts/{debtId}/payment/dispute")
    public PaymentResponse disputeByDebt(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser, @PathVariable long debtId) {
        return PaymentResponse.from(paymentService.disputeForDebt(debtId, requireUserId(authenticatedUser)));
    }

    private static long requireUserId(AuthenticatedUser authenticatedUser) {
        if (authenticatedUser == null) {
            throw new DomainException(ErrorCode.AUTHENTICATION_ERROR, "User is not authenticated");
        }
        return authenticatedUser.getUserId();
    }
}
