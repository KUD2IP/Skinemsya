package skinemsya.vse.ru.api.internal.service.receipts;

import java.util.List;
import skinemsya.vse.ru.api.internal.pojo.receipts.PositionData;
import skinemsya.vse.ru.api.internal.pojo.receipts.SelectionData;
import skinemsya.vse.ru.api.internal.pojo.receipts.SharedTargetData;

/** Данные чека, нужные для расчёта долгов. */
public interface ReceiptDataPort {

    /** Возвращает позиции сбора. */
    List<PositionData> getPositions(long eventId);

    /** Возвращает выбор позиций участниками. */
    List<SelectionData> getSelections(long eventId);

    /** Возвращает адресатов общих позиций. */
    List<SharedTargetData> getSharedTargets(long eventId);
}
