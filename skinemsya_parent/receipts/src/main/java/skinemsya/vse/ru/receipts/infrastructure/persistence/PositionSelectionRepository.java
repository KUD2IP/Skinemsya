package skinemsya.vse.ru.receipts.infrastructure.persistence;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PositionSelectionRepository extends JpaRepository<PositionSelectionEntity, Long> {

    List<PositionSelectionEntity> findByPositionId(long positionId);

    List<PositionSelectionEntity> findByPositionIdIn(List<Long> positionIds);

    Optional<PositionSelectionEntity> findByPositionIdAndUserId(long positionId, long userId);

    void deleteByPositionId(long positionId);

    @Query(
            """
            SELECT COALESCE(SUM(ps.selectedQuantity), 0)
            FROM PositionSelectionEntity ps
            WHERE ps.positionId = :positionId AND ps.userId <> :userId
            """)
    BigDecimal sumSelectedByOthers(@Param("positionId") long positionId, @Param("userId") long userId);
}
