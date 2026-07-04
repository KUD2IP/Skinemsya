package skinemsya.vse.ru.receipts.application;

import skinemsya.vse.ru.receipts.domain.Position;

import java.math.BigDecimal;
import java.util.List;

public interface PositionService {

    Position addManual(long eventId, long userId, String name, BigDecimal quantity, long totalPriceKopecks);

    Position update(long positionId, long userId, String name, BigDecimal quantity, long totalPriceKopecks);

    void delete(long positionId, long userId);

    List<Position> listByEvent(long eventId, long userId);

    Position markShared(long positionId, long userId, boolean forAll, List<Long> targetUserIds);

    long countByEvent(long eventId);
}
