package skinemsya.vse.ru.payments.application;

import org.springframework.stereotype.Component;
import skinemsya.vse.ru.debts.infrastructure.persistence.DebtRepository;
import skinemsya.vse.ru.files.application.FileSharedAccessVerifier;
import skinemsya.vse.ru.payments.infrastructure.persistence.PaymentRepository;

@Component
public class PaymentProofFileAccessVerifier implements FileSharedAccessVerifier {

    private final PaymentRepository paymentRepository;
    private final DebtRepository debtRepository;

    public PaymentProofFileAccessVerifier(PaymentRepository paymentRepository, DebtRepository debtRepository) {
        this.paymentRepository = paymentRepository;
        this.debtRepository = debtRepository;
    }

    @Override
    public boolean canAccess(long fileId, long requesterId) {
        return paymentRepository
                .findByScreenshotFileId(fileId)
                .flatMap(payment -> debtRepository.findById(payment.getDebtId()))
                .filter(debt -> debt.getCreditorId() == requesterId)
                .isPresent();
    }
}
