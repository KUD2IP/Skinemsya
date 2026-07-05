package skinemsya.vse.ru.receipts.infrastructure.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SharedPositionTargetRepository extends JpaRepository<SharedPositionTargetEntity, Long> {

    List<SharedPositionTargetEntity> findByPositionId(long positionId);

    List<SharedPositionTargetEntity> findByPositionIdIn(List<Long> positionIds);

    void deleteByPositionId(long positionId);
}
