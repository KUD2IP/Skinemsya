package skinemsya.vse.ru.subscriptions.infrastructure.persistence;

import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ParseUsageRepository extends JpaRepository<ParseUsageEntity, Long> {

    Optional<ParseUsageEntity> findByUserIdAndUsageDate(long userId, LocalDate usageDate);
}
