package skinemsya.vse.ru.receipts.application;

/** Доступ к платному распознаванию чека. Реализация живёт в модуле подписки. */
public interface ReceiptParseAccess {

    void authorizeParse(long userId);

    /** Удачное чтение чека без подписки тратит бесплатную попытку. Неудачное фото не тратит. */
    void recordSuccessfulParse(long userId);

    void recordModelCall(long userId);
}
