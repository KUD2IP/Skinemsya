package skinemsya.vse.ru.payments.application;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import skinemsya.vse.ru.common.event.DebtorConfirmed;
import skinemsya.vse.ru.common.event.PaymentDisputed;
import skinemsya.vse.ru.debts.application.DebtService;
import skinemsya.vse.ru.debts.domain.DebtStatus;
import skinemsya.vse.ru.debts.domain.exception.DebtNotFoundException;
import skinemsya.vse.ru.debts.infrastructure.persistence.DebtRepository;
import skinemsya.vse.ru.events.application.EventAccessPort;
import skinemsya.vse.ru.files.application.FileService;
import skinemsya.vse.ru.payments.domain.Payment;
import skinemsya.vse.ru.payments.domain.PaymentDetailsView;
import skinemsya.vse.ru.payments.domain.PaymentStatus;
import skinemsya.vse.ru.payments.domain.exception.PayerPaymentDetailsMissingException;
import skinemsya.vse.ru.payments.domain.exception.PaymentAccessDeniedException;
import skinemsya.vse.ru.payments.domain.exception.PaymentInvalidStateException;
import skinemsya.vse.ru.payments.domain.exception.PaymentNotFoundException;
import skinemsya.vse.ru.payments.infrastructure.persistence.PayerReminderJobEntity;
import skinemsya.vse.ru.payments.infrastructure.persistence.PayerReminderJobRepository;
import skinemsya.vse.ru.payments.infrastructure.persistence.PaymentEntity;
import skinemsya.vse.ru.payments.infrastructure.persistence.PaymentRepository;
import skinemsya.vse.ru.users.application.UserService;

@Service
@Transactional
public class PaymentServiceImpl implements PaymentService {

    private static final Duration REMINDER_DELAY = Duration.ofHours(2);

    private final PaymentRepository paymentRepository;
    private final DebtRepository debtRepository;
    private final DebtService debtService;
    private final UserService userService;
    private final FileService fileService;
    private final EventAccessPort eventAccessPort;
    private final PayerReminderJobRepository reminderJobRepository;
    private final ApplicationEventPublisher eventPublisher;

    public PaymentServiceImpl(
            PaymentRepository paymentRepository,
            DebtRepository debtRepository,
            DebtService debtService,
            UserService userService,
            FileService fileService,
            EventAccessPort eventAccessPort,
            PayerReminderJobRepository reminderJobRepository,
            ApplicationEventPublisher eventPublisher) {
        this.paymentRepository = paymentRepository;
        this.debtRepository = debtRepository;
        this.debtService = debtService;
        this.userService = userService;
        this.fileService = fileService;
        this.eventAccessPort = eventAccessPort;
        this.reminderJobRepository = reminderJobRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentDetailsView getPaymentDetails(long debtId, long requesterId) {
        var debt = debtRepository.findById(debtId).orElseThrow(DebtNotFoundException::new);
        if (debt.getDebtorId() != requesterId) {
            throw new PaymentAccessDeniedException();
        }
        eventAccessPort.requireParticipant(debt.getEventId(), requesterId);

        var creditor = userService.findById(debt.getCreditorId()).orElseThrow(DebtNotFoundException::new);
        var paymentDetails = userService.getPaymentDetails(debt.getCreditorId());
        if (paymentDetails.paymentDetails() == null
                || paymentDetails.paymentDetails().isBlank()) {
            throw new PayerPaymentDetailsMissingException();
        }

        var payment = paymentRepository.findByDebtId(debtId).orElse(null);
        return new PaymentDetailsView(
                debtId,
                debt.getEventId(),
                debt.getAmountKopecks(),
                debt.getCreditorId(),
                creditor.displayName(),
                paymentDetails.paymentDetails(),
                paymentDetails.phone(),
                paymentDetails.preferredBank(),
                payment != null ? payment.getStatus() : PaymentStatus.CREATED);
    }

    @Override
    public Payment confirmByDebtor(long debtId, long debtorId, long screenshotFileId) {
        var debt = debtRepository.findById(debtId).orElseThrow(DebtNotFoundException::new);
        if (debt.getDebtorId() != debtorId) {
            throw new PaymentAccessDeniedException();
        }
        if (debt.getStatus() == DebtStatus.PAID) {
            throw new PaymentInvalidStateException();
        }

        fileService.requireOwnerOrShared(screenshotFileId, debtorId, true);

        var payment = paymentRepository.findByDebtId(debtId).orElseGet(() -> createPayment(debtId));
        if (payment.getStatus() == PaymentStatus.PAYER_CONFIRMED) {
            return toDomain(payment);
        }
        if (payment.getStatus() != PaymentStatus.CREATED && payment.getStatus() != PaymentStatus.DISPUTED) {
            if (payment.getStatus() == PaymentStatus.DEBTOR_CONFIRMED) {
                return toDomain(payment);
            }
            throw new PaymentInvalidStateException();
        }

        var now = Instant.now();
        payment.setScreenshotFileId(screenshotFileId);
        payment.setStatus(PaymentStatus.DEBTOR_CONFIRMED);
        payment.setDebtorConfirmedAt(now);
        payment.setUpdatedAt(now);
        payment = paymentRepository.save(payment);

        debtService.markPendingConfirmation(debtId);
        scheduleReminder(payment.getId(), now);

        var debtor = userService.findById(debtorId).orElseThrow(DebtNotFoundException::new);
        eventPublisher.publishEvent(new DebtorConfirmed(
                debt.getEventId(),
                payment.getId(),
                debtorId,
                debt.getCreditorId(),
                debt.getAmountKopecks(),
                debtor.displayName()));
        return toDomain(payment);
    }

    @Override
    public List<Payment> confirmAllForEvent(long eventId, long payerId) {
        if (eventAccessPort.getPayerId(eventId) != payerId) {
            throw new PaymentAccessDeniedException();
        }

        var debts = debtRepository.findByEventId(eventId);
        List<Payment> confirmed = new ArrayList<>();
        for (var debt : debts) {
            if (debt.getCreditorId() != payerId) {
                continue;
            }
            paymentRepository.findByDebtId(debt.getId()).ifPresent(payment -> {
                if (payment.getStatus() == PaymentStatus.DEBTOR_CONFIRMED) {
                    confirmed.add(confirmPaymentInternal(payment, debt.getId(), eventId));
                }
            });
        }
        maybeCompleteEvent(eventId);
        return confirmed;
    }

    @Override
    public Payment confirmByPayer(long paymentId, long payerId) {
        var payment = paymentRepository.findById(paymentId).orElseThrow(PaymentNotFoundException::new);
        return confirmByPayerInternal(payment, payerId);
    }

    @Override
    public Payment confirmByPayerForDebt(long debtId, long payerId) {
        var payment = paymentRepository.findByDebtId(debtId).orElseThrow(PaymentNotFoundException::new);
        return confirmByPayerInternal(payment, payerId);
    }

    private Payment confirmByPayerInternal(PaymentEntity payment, long payerId) {
        var debt = debtRepository.findById(payment.getDebtId()).orElseThrow(DebtNotFoundException::new);
        if (debt.getCreditorId() != payerId) {
            throw new PaymentAccessDeniedException();
        }
        if (payment.getStatus() == PaymentStatus.PAYER_CONFIRMED) {
            return toDomain(payment);
        }
        if (payment.getStatus() != PaymentStatus.DEBTOR_CONFIRMED) {
            throw new PaymentInvalidStateException();
        }

        var confirmed = confirmPaymentInternal(payment, debt.getId(), debt.getEventId());
        maybeCompleteEvent(debt.getEventId());
        return confirmed;
    }

    @Override
    public Payment dispute(long paymentId, long payerId) {
        var payment = paymentRepository.findById(paymentId).orElseThrow(PaymentNotFoundException::new);
        return disputeInternal(payment, payerId);
    }

    @Override
    public Payment disputeForDebt(long debtId, long payerId) {
        var payment = paymentRepository.findByDebtId(debtId).orElseThrow(PaymentNotFoundException::new);
        return disputeInternal(payment, payerId);
    }

    private Payment disputeInternal(PaymentEntity payment, long payerId) {
        var debt = debtRepository.findById(payment.getDebtId()).orElseThrow(DebtNotFoundException::new);
        if (debt.getCreditorId() != payerId) {
            throw new PaymentAccessDeniedException();
        }
        if (payment.getStatus() == PaymentStatus.DISPUTED) {
            return toDomain(payment);
        }
        if (payment.getStatus() != PaymentStatus.DEBTOR_CONFIRMED) {
            throw new PaymentInvalidStateException();
        }

        payment.setStatus(PaymentStatus.DISPUTED);
        payment.setUpdatedAt(Instant.now());
        payment = paymentRepository.save(payment);

        eventPublisher.publishEvent(new PaymentDisputed(
                debt.getEventId(), payment.getId(), debt.getDebtorId(), debt.getCreditorId(), debt.getAmountKopecks()));
        return toDomain(payment);
    }

    private Payment confirmPaymentInternal(PaymentEntity payment, long debtId, long eventId) {
        var now = Instant.now();
        payment.setStatus(PaymentStatus.PAYER_CONFIRMED);
        payment.setPayerConfirmedAt(now);
        payment.setUpdatedAt(now);
        payment = paymentRepository.save(payment);
        debtService.close(debtId);
        return toDomain(payment);
    }

    private void maybeCompleteEvent(long eventId) {
        if (debtService.allPaidForEvent(eventId)) {
            eventAccessPort.tryMarkCompleted(eventId);
        }
    }

    private PaymentEntity createPayment(long debtId) {
        var now = Instant.now();
        var entity = new PaymentEntity();
        entity.setDebtId(debtId);
        entity.setStatus(PaymentStatus.CREATED);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        return paymentRepository.save(entity);
    }

    private void scheduleReminder(long paymentId, Instant from) {
        if (reminderJobRepository.findByPaymentId(paymentId).isEmpty()) {
            var job = new PayerReminderJobEntity();
            job.setPaymentId(paymentId);
            job.setScheduledAt(from.plus(REMINDER_DELAY));
            reminderJobRepository.save(job);
        }
    }

    private static Payment toDomain(PaymentEntity entity) {
        return new Payment(
                entity.getId(),
                entity.getDebtId(),
                entity.getStatus(),
                entity.getScreenshotFileId(),
                entity.getDebtorConfirmedAt(),
                entity.getPayerConfirmedAt(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }
}
