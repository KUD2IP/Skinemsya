package skinemsya.vse.ru.impl.service.payments;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import skinemsya.vse.ru.api.internal.repository.debts.DebtRepository;
import skinemsya.vse.ru.api.internal.repository.payments.PaymentRepository;
import skinemsya.vse.ru.api.internal.service.files.FileSharedAccessVerifier;

/** Кредитор долга может открыть скриншот перевода. */
@Component
@RequiredArgsConstructor
public class PaymentProofFileAccessVerifier implements FileSharedAccessVerifier {

    private final PaymentRepository paymentRepository;
    private final DebtRepository debtRepository;

    @Override
    public boolean canAccess(long fileId, long requesterId) {
        return paymentRepository
                .findByScreenshotFileId(fileId)
                .flatMap(payment -> debtRepository.findById(payment.getDebtId()))
                .filter(debt -> debt.getCreditorId() == requesterId)
                .isPresent();
    }
}
