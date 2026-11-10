package skinemsya.vse.ru.app.predictions.infrastructure;

import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EveningDrawJpaRepository extends JpaRepository<EveningDrawEntity, Long> {

    List<EveningDrawEntity> findByTelegramChatIdAndCreatedAtGreaterThanEqualOrderByCreatedAtAsc(
            long telegramChatId, Instant createdAt);
}
