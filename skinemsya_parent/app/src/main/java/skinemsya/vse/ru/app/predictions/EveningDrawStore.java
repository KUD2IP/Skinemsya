package skinemsya.vse.ru.app.predictions;

import java.time.Instant;
import java.util.List;

public interface EveningDrawStore {

    void remember(long chatId, ChatPerson person, Instant seenAt);

    List<ChatPerson> speakers(long chatId);

    List<EveningDraw> drawsSince(long chatId, Instant since);

    void saveDraw(long chatId, String frameId, long targetTelegramUserId, Instant createdAt);
}
