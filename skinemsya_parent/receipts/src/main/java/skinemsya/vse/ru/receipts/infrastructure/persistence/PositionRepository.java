package skinemsya.vse.ru.receipts.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface PositionRepository extends JpaRepository<PositionEntity, Long> {

    List<PositionEntity> findByEventIdOrderByCreatedAtAsc(long eventId);

    long countByEventId(long eventId);

    @Query("SELECT COALESCE(SUM(p.totalPriceKopecks), 0) FROM PositionEntity p WHERE p.eventId = :eventId")
    long sumTotalPriceKopecksByEventId(long eventId);

    Optional<PositionEntity> findByIdAndEventId(long id, long eventId);
}
