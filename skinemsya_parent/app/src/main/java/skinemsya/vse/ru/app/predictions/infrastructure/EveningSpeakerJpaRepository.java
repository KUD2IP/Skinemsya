package skinemsya.vse.ru.app.predictions.infrastructure;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EveningSpeakerJpaRepository extends JpaRepository<EveningSpeakerEntity, Long> {

    Optional<EveningSpeakerEntity> findByTelegramChatIdAndTelegramUserId(long telegramChatId, long telegramUserId);

    List<EveningSpeakerEntity> findByTelegramChatIdOrderByLastSeenAtAsc(long telegramChatId);
}
