package skinemsya.vse.ru.receipts.application;

import skinemsya.vse.ru.receipts.domain.Receipt;

public interface ReceiptService {

    Receipt processReceipt(long eventId, long fileId, long userId);

    Receipt splitTips(long eventId, long receiptId, long userId);

    java.util.List<Receipt> listByEvent(long eventId, long userId);
}
