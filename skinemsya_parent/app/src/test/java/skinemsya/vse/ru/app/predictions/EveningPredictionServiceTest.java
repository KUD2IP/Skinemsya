package skinemsya.vse.ru.app.predictions;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.random.RandomGenerator;
import org.junit.jupiter.api.Test;

class EveningPredictionServiceTest {

    private static final long CHAT = -100L;
    private static final ChatPerson BOB = new ChatPerson(2L, "Bob", "bob_ok");
    private static final ChatPerson ANN = new ChatPerson(9L, "Ann <bar>", "ab");

    private final MemoryStore store = new MemoryStore();
    private final EveningFrameCatalog catalog = new EveningFrameCatalog();

    @Test
    void shouldDrawDuringTheDay() {
        String reply = service("2026-11-10T14:59:00Z").reply(CHAT, BOB, null);

        assertThat(reply).contains("@bob_ok");
        assertThat(store.draws).hasSize(1);
        assertThat(store.speakers(CHAT)).containsExactly(BOB);
    }

    @Test
    void shouldDrawAtOpeningAndMentionByUsername() {
        String reply = service("2026-11-10T15:00:00Z").reply(CHAT, BOB, null);

        assertThat(reply)
                .contains("@bob_ok")
                .contains("21:40")
                .doesNotContain("{user}")
                .doesNotContain("{time}");
        assertThat(store.draws).hasSize(1);
        assertThat(store.draws.get(0).frameId()).isEqualTo("one-glass");
        assertThat(store.draws.get(0).targetTelegramUserId()).isEqualTo(2L);
    }

    @Test
    void shouldDrawAgainImmediately() {
        EveningPredictionService service = service("2026-11-10T09:00:00Z");

        assertThat(service.reply(CHAT, BOB, null)).contains("@bob_ok");
        assertThat(service.reply(CHAT, BOB, null)).contains("@bob_ok");

        assertThat(store.draws).hasSize(2);
        assertThat(store.draws.get(1).frameId()).isEqualTo("ex-talk");
    }

    @Test
    void shouldSkipFrameUsedInTheLastNinetyDays() {
        store.saveDraw(CHAT, "one-glass", 2L, Instant.parse("2026-11-10T16:00:00Z"));

        String reply = service("2026-11-10T18:00:00Z").reply(CHAT, BOB, null);

        assertThat(store.draws.get(1).frameId()).isEqualTo("ex-talk");
        assertThat(reply).contains("бывшей");
    }

    @Test
    void shouldLetAnExplicitReplyRepeatThePreviousPerson() {
        store.remember(CHAT, ANN, Instant.parse("2026-11-10T15:00:00Z"));
        store.saveDraw(CHAT, "one-glass", 9L, Instant.parse("2026-11-10T16:00:00Z"));

        String reply = service("2026-11-10T18:00:00Z").reply(CHAT, BOB, ANN);

        assertThat(reply).contains("tg://user?id=9").contains("Ann &lt;bar&gt;").doesNotContain("@ab");
        assertThat(store.draws.get(1).targetTelegramUserId()).isEqualTo(9L);
    }

    @Test
    void shouldNameTwoDifferentPeople() {
        EveningFrameCatalog pairs = catalogWith(new EveningFrame(
                "wine-on-both",
                "alcohol",
                "adult",
                "Сегодня вечером {user} обольёт вином {other}, а закончат они уже в одной постели."));
        store.remember(CHAT, BOB, Instant.parse("2026-11-10T12:00:00Z"));
        store.remember(CHAT, new ChatPerson(9L, "Ann", "ann_ok"), Instant.parse("2026-11-10T12:01:00Z"));

        String reply = service(pairs, "2026-11-10T15:00:00Z").reply(CHAT, BOB, null);

        assertThat(reply)
                .contains("@bob_ok")
                .contains("@ann_ok")
                .doesNotContain("{user}")
                .doesNotContain("{other}");
    }

    @Test
    void shouldKeepASingleNameWhenNobodyElseIsKnown() {
        EveningFrameCatalog mixed = catalogWith(
                new EveningFrame(
                        "wine-on-both",
                        "alcohol",
                        "adult",
                        "Сегодня вечером {user} обольёт вином {other}, а закончат они уже в одной постели."),
                new EveningFrame(
                        "one-glass",
                        "alcohol",
                        "mild",
                        "Сегодня вечером {user} возьмёт один бокал и назовёт это характером."));

        String reply = service(mixed, "2026-11-10T15:00:00Z").reply(CHAT, BOB, null);

        assertThat(reply).contains("@bob_ok").contains("один бокал").doesNotContain("{other}");
    }

    @Test
    void shouldNotPickTheSamePersonTwiceWhenNobodyIsRepliedTo() {
        store.remember(CHAT, BOB, Instant.parse("2026-11-10T15:00:00Z"));
        store.remember(CHAT, ANN, Instant.parse("2026-11-10T15:01:00Z"));
        store.saveDraw(CHAT, "one-glass", 2L, Instant.parse("2026-11-10T16:00:00Z"));

        service("2026-11-10T18:00:00Z").reply(CHAT, BOB, null);

        assertThat(store.draws.get(1).targetTelegramUserId()).isEqualTo(9L);
    }

    private EveningPredictionService service(String instant) {
        return service(catalog, instant);
    }

    private EveningPredictionService service(EveningFrameCatalog frames, String instant) {
        return new EveningPredictionService(
                store, frames, Clock.fixed(Instant.parse(instant), EveningPredictionService.ZONE), new FixedRandom());
    }

    private static EveningFrameCatalog catalogWith(EveningFrame... frames) {
        return new EveningFrameCatalog(new EveningFrameCatalog.Loaded(List.of("21:40"), List.of(frames)));
    }

    private static final class FixedRandom implements RandomGenerator {
        @Override
        public long nextLong() {
            return 0;
        }

        @Override
        public int nextInt(int bound) {
            return 0;
        }
    }

    private static final class MemoryStore implements EveningDrawStore {
        private final Map<Long, Map<Long, ChatPerson>> people = new LinkedHashMap<>();
        private final List<EveningDraw> draws = new ArrayList<>();

        @Override
        public void remember(long chatId, ChatPerson person, Instant seenAt) {
            people.computeIfAbsent(chatId, ignored -> new LinkedHashMap<>()).put(person.telegramUserId(), person);
        }

        @Override
        public List<ChatPerson> speakers(long chatId) {
            return List.copyOf(people.getOrDefault(chatId, Map.of()).values());
        }

        @Override
        public List<EveningDraw> drawsSince(long chatId, Instant since) {
            return draws.stream()
                    .filter(draw -> !draw.createdAt().isBefore(since))
                    .sorted(Comparator.comparing(EveningDraw::createdAt))
                    .toList();
        }

        @Override
        public void saveDraw(long chatId, String frameId, long targetTelegramUserId, Instant createdAt) {
            draws.add(new EveningDraw(frameId, targetTelegramUserId, createdAt));
        }
    }
}
