package skinemsya.vse.ru.impl.service.payments;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import skinemsya.vse.ru.api.internal.enums.ErrorCode;
import skinemsya.vse.ru.api.internal.enums.debts.DebtStatus;
import skinemsya.vse.ru.api.internal.enums.payments.PaymentStatus;
import skinemsya.vse.ru.api.internal.event.payments.DebtorConfirmed;
import skinemsya.vse.ru.api.internal.event.payments.PaymentDisputed;
import skinemsya.vse.ru.api.internal.ex.DomainException;
import skinemsya.vse.ru.api.internal.pojo.debts.Debt;
import skinemsya.vse.ru.api.internal.pojo.payments.Payment;
import skinemsya.vse.ru.api.internal.pojo.payments.PaymentDetailsView;
import skinemsya.vse.ru.api.internal.pojo.users.PayoutRequisites;
import skinemsya.vse.ru.api.internal.repository.debts.DebtRepository;
import skinemsya.vse.ru.api.internal.repository.payments.PaymentRepository;
import skinemsya.vse.ru.api.internal.service.debts.DebtService;
import skinemsya.vse.ru.api.internal.service.events.EventAccessPort;
import skinemsya.vse.ru.api.internal.service.files.FileService;
import skinemsya.vse.ru.api.internal.service.payments.PaymentService;
import skinemsya.vse.ru.api.internal.service.users.UserService;

/** Подтверждение переводов должником и плательщиком. */
@Service
@Transactional
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final DebtRepository debtRepository;
    private final DebtService debtService;
    private final UserService userService;
    private final FileService fileService;
    private final EventAccessPort eventAccessPort;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional(readOnly = true)
    public PaymentDetailsView getPaymentDetails(long debtId, long requesterId) {
        var debt = requireDebt(debtId);
        if (debt.getDebtorId() != requesterId) {
            throw denied();
        }
        eventAccessPort.requireParticipant(debt.getEventId(), requesterId);

        var creditor = userService.findById(debt.getCreditorId()).orElseThrow(this::debtNotFound);
        var paymentDetails = userService.getPaymentDetails(debt.getCreditorId());
        if (!PayoutRequisites.hasAny(paymentDetails)) {
            throw new DomainException(
                    ErrorCode.DOMAIN_RULE_VIOLATION, "Payer payment details are required before distribution");
        }

        var payment = paymentRepository.findByDebtId(debtId).orElse(null);
        return new PaymentDetailsView(
                debtId,
                debt.getEventId(),
                debt.getAmountKopecks(),
                debt.getCreditorId(),
                creditor.getDisplayName(),
                paymentDetails.getPaymentDetails(),
                paymentDetails.getPhone(),
                paymentDetails.getPreferredBank(),
                payment != null ? payment.getStatus() : PaymentStatus.CREATED);
    }

    @Override
    public Payment confirmByDebtor(long debtId, long debtorId, Long screenshotFileId) {
        var debt = requireDebt(debtId);
        if (debt.getDebtorId() != debtorId) {
            throw denied();
        }
        if (debt.getStatus() == DebtStatus.PAID) {
            throw invalidState();
        }

        if (screenshotFileId != null) {
            fileService.requireOwnerOrShared(screenshotFileId, debtorId, true);
        }

        var payment = paymentRepository.findByDebtId(debtId).orElseGet(() -> createPayment(debtId));
        if (payment.getStatus() == PaymentStatus.PAYER_CONFIRMED) {
            return payment;
        }
        if (payment.getStatus() != PaymentStatus.CREATED && payment.getStatus() != PaymentStatus.DISPUTED) {
            if (payment.getStatus() == PaymentStatus.DEBTOR_CONFIRMED) {
                return payment;
            }
            throw invalidState();
        }

        var now = Instant.now();
        if (screenshotFileId != null) {
            payment.setScreenshotFileId(screenshotFileId);
        }
        payment.setStatus(PaymentStatus.DEBTOR_CONFIRMED);
        payment.setDebtorConfirmedAt(now);
        payment.setUpdatedAt(now);
        payment = paymentRepository.save(payment);

        debtService.markPendingConfirmation(debtId);

        var debtor = userService.findById(debtorId).orElseThrow(this::debtNotFound);
        eventPublisher.publishEvent(new DebtorConfirmed(
                debt.getEventId(),
                payment.getId(),
                debtorId,
                debt.getCreditorId(),
                debt.getAmountKopecks(),
                debtor.getDisplayName()));
        return payment;
    }

    @Override
    public List<Payment> confirmAllForEvent(long eventId, long payerId) {
        if (eventAccessPort.getPayerId(eventId) != payerId) {
            throw denied();
        }

        var debts = debtRepository.findByEventId(eventId);
        List<Payment> confirmed = new ArrayList<>();
        for (var debt : debts) {
            if (debt.getCreditorId() != payerId) {
                continue;
            }
            paymentRepository.findByDebtId(debt.getId()).ifPresent(payment -> {
                if (payment.getStatus() == PaymentStatus.DEBTOR_CONFIRMED
                        || payment.getStatus() == PaymentStatus.DISPUTED) {
                    confirmed.add(confirmPaymentInternal(payment, debt.getId()));
                }
            });
        }
        return confirmed;
    }

    @Override
    public Payment confirmByPayer(long paymentId, long payerId) {
        var payment = paymentRepository.findById(paymentId).orElseThrow(this::paymentNotFound);
        return confirmByPayerInternal(payment, payerId);
    }

    @Override
    public Payment confirmByPayerForDebt(long debtId, long payerId) {
        var payment = paymentRepository.findByDebtId(debtId).orElseThrow(this::paymentNotFound);
        return confirmByPayerInternal(payment, payerId);
    }

    @Override
    public Payment dispute(long paymentId, long payerId) {
        var payment = paymentRepository.findById(paymentId).orElseThrow(this::paymentNotFound);
        return disputeInternal(payment, payerId);
    }

    @Override
    public Payment disputeForDebt(long debtId, long payerId) {
        var payment = paymentRepository.findByDebtId(debtId).orElseThrow(this::paymentNotFound);
        return disputeInternal(payment, payerId);
    }

    private Payment confirmByPayerInternal(Payment payment, long payerId) {
        var debt = requireDebt(payment.getDebtId());
        if (debt.getCreditorId() != payerId) {
            throw denied();
        }
        if (payment.getStatus() == PaymentStatus.PAYER_CONFIRMED) {
            return payment;
        }
        if (payment.getStatus() != PaymentStatus.DEBTOR_CONFIRMED && payment.getStatus() != PaymentStatus.DISPUTED) {
            throw invalidState();
        }
        return confirmPaymentInternal(payment, debt.getId());
    }

    private Payment disputeInternal(Payment payment, long payerId) {
        var debt = requireDebt(payment.getDebtId());
        if (debt.getCreditorId() != payerId) {
            throw denied();
        }
        if (payment.getStatus() == PaymentStatus.DISPUTED) {
            return payment;
        }
        if (payment.getStatus() != PaymentStatus.DEBTOR_CONFIRMED) {
            throw invalidState();
        }

        payment.setStatus(PaymentStatus.DISPUTED);
        payment.setUpdatedAt(Instant.now());
        payment = paymentRepository.save(payment);

        eventPublisher.publishEvent(new PaymentDisputed(
                debt.getEventId(), payment.getId(), debt.getDebtorId(), debt.getCreditorId(), debt.getAmountKopecks()));
        return payment;
    }

    private Payment confirmPaymentInternal(Payment payment, long debtId) {
        var now = Instant.now();
        payment.setStatus(PaymentStatus.PAYER_CONFIRMED);
        payment.setPayerConfirmedAt(now);
        payment.setUpdatedAt(now);
        payment = paymentRepository.save(payment);
        debtService.close(debtId);
        return payment;
    }

    private Payment createPayment(long debtId) {
        var now = Instant.now();
        return paymentRepository.save(Payment.builder()
                .debtId(debtId)
                .status(PaymentStatus.CREATED)
                .createdAt(now)
                .updatedAt(now)
                .build());
    }

    private Debt requireDebt(long debtId) {
        return debtRepository.findById(debtId).orElseThrow(this::debtNotFound);
    }

    private DomainException debtNotFound() {
        return new DomainException(ErrorCode.NOT_FOUND, "Debt not found");
    }

    private DomainException paymentNotFound() {
        return new DomainException(ErrorCode.NOT_FOUND, "Payment not found");
    }

    private static DomainException denied() {
        return new DomainException(ErrorCode.AUTHORIZATION_ERROR, "Payment access denied");
    }

    private static DomainException invalidState() {
        return new DomainException(ErrorCode.DOMAIN_CONFLICT, "Payment is not in a valid state for this operation");
    }
}
