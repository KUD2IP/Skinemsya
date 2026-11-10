package skinemsya.vse.ru.app.predictions.infrastructure;

import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Repository;
import skinemsya.vse.ru.app.predictions.ChatPerson;
import skinemsya.vse.ru.app.predictions.EveningDraw;
import skinemsya.vse.ru.app.predictions.EveningDrawStore;

@Repository
public class JpaEveningDrawStore implements EveningDrawStore {

    private final EveningSpeakerJpaRepository speakers;
    private final EveningDrawJpaRepository draws;

    public JpaEveningDrawStore(EveningSpeakerJpaRepository speakers, EveningDrawJpaRepository draws) {
        this.speakers = speakers;
        this.draws = draws;
    }

    @Override
    public void remember(long chatId, ChatPerson person, Instant seenAt) {
        EveningSpeakerEntity entity = speakers.findByTelegramChatIdAndTelegramUserId(chatId, person.telegramUserId())
                .orElseGet(EveningSpeakerEntity::new);
        entity.setTelegramChatId(chatId);
        entity.setTelegramUserId(person.telegramUserId());
        entity.setDisplayName(person.displayName());
        entity.setUsername(person.username());
        entity.setLastSeenAt(seenAt);
        speakers.save(entity);
    }

    @Override
    public List<ChatPerson> speakers(long chatId) {
        return speakers.findByTelegramChatIdOrderByLastSeenAtAsc(chatId).stream()
                .map(entity ->
                        new ChatPerson(entity.getTelegramUserId(), entity.getDisplayName(), entity.getUsername()))
                .toList();
    }

    @Override
    public List<EveningDraw> drawsSince(long chatId, Instant since) {
        return draws.findByTelegramChatIdAndCreatedAtGreaterThanEqualOrderByCreatedAtAsc(chatId, since).stream()
                .map(entity ->
                        new EveningDraw(entity.getFrameId(), entity.getTargetTelegramUserId(), entity.getCreatedAt()))
                .toList();
    }

    @Override
    public void saveDraw(long chatId, String frameId, long targetTelegramUserId, Instant createdAt) {
        EveningDrawEntity entity = new EveningDrawEntity();
        entity.setTelegramChatId(chatId);
        entity.setFrameId(frameId);
        entity.setTargetTelegramUserId(targetTelegramUserId);
        entity.setCreatedAt(createdAt);
        draws.save(entity);
    }
}
