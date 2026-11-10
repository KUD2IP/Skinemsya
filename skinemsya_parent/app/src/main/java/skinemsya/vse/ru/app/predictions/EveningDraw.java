package skinemsya.vse.ru.app.predictions;

import java.time.Instant;

public record EveningDraw(String frameId, long targetTelegramUserId, Instant createdAt) {}
