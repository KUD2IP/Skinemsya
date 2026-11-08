package skinemsya.vse.ru.receipts.application;

/** Доступ к платному распознаванию чека. Реализация живёт в модуле подписки. */
public interface ReceiptParseAccess {

    void authorizeParse(long userId);

    void recordModelCall(long userId);
}
