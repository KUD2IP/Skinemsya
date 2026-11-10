package skinemsya.vse.ru.app.predictions;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.random.RandomGenerator;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EveningPredictionService {

    static final ZoneId ZONE = ZoneId.of("Europe/Moscow");
    static final Duration REPEAT_WINDOW = Duration.ofDays(90);

    private static final Pattern USERNAME = Pattern.compile("[A-Za-z0-9_]{5,32}");

    private final EveningDrawStore store;
    private final EveningFrameCatalog catalog;
    private final Clock clock;
    private final RandomGenerator random;

    @Autowired
    public EveningPredictionService(EveningDrawStore store, EveningFrameCatalog catalog) {
        this(store, catalog, Clock.system(ZONE), new Random());
    }

    EveningPredictionService(EveningDrawStore store, EveningFrameCatalog catalog, Clock clock, RandomGenerator random) {
        this.store = store;
        this.catalog = catalog;
        this.clock = clock;
        this.random = random;
    }

    @Transactional
    public void remember(long chatId, ChatPerson person) {
        if (person == null) {
            return;
        }
        store.remember(chatId, person, clock.instant());
    }

    @Transactional
    public String reply(long chatId, ChatPerson caller, ChatPerson replyTo) {
        Instant now = clock.instant();
        remember(chatId, caller);
        remember(chatId, replyTo);

        List<EveningDraw> draws = store.drawsSince(chatId, now.minus(REPEAT_WINDOW));
        ChatPerson target = chooseTarget(chatId, caller, replyTo, draws);
        ChatPerson other = chooseOther(chatId, target, caller);
        EveningFrame frame = chooseFrame(draws, other != null);
        store.saveDraw(chatId, frame.id(), target.telegramUserId(), now);
        return render(frame, target, other);
    }

    private ChatPerson chooseTarget(long chatId, ChatPerson caller, ChatPerson replyTo, List<EveningDraw> draws) {
        if (replyTo != null) {
            return replyTo;
        }
        List<ChatPerson> pool = new ArrayList<>(store.speakers(chatId));
        if (pool.isEmpty() && caller != null) {
            pool.add(caller);
        }
        if (pool.isEmpty()) {
            throw new IllegalStateException("Evening prediction has nobody to address");
        }
        if (pool.size() > 1 && !draws.isEmpty()) {
            long lastTarget = draws.get(draws.size() - 1).targetTelegramUserId();
            List<ChatPerson> others = pool.stream()
                    .filter(person -> person.telegramUserId() != lastTarget)
                    .toList();
            if (!others.isEmpty()) {
                pool = others;
            }
        }
        return pool.get(random.nextInt(pool.size()));
    }

    private ChatPerson chooseOther(long chatId, ChatPerson target, ChatPerson caller) {
        List<ChatPerson> pool = new ArrayList<>(store.speakers(chatId).stream()
                .filter(person -> person.telegramUserId() != target.telegramUserId())
                .toList());
        if (pool.isEmpty() && caller != null && caller.telegramUserId() != target.telegramUserId()) {
            pool.add(caller);
        }
        if (pool.isEmpty()) {
            return null;
        }
        return pool.get(random.nextInt(pool.size()));
    }

    private EveningFrame chooseFrame(List<EveningDraw> draws, boolean canPair) {
        List<EveningFrame> fitting = catalog.frames().stream()
                .filter(frame -> canPair || !needsOther(frame))
                .toList();
        if (fitting.isEmpty()) {
            throw new IllegalStateException("Evening prediction has nobody to pair");
        }
        List<String> used = draws.stream().map(EveningDraw::frameId).toList();
        List<EveningFrame> available =
                fitting.stream().filter(frame -> !used.contains(frame.id())).toList();
        if (available.isEmpty()) {
            String latest = used.isEmpty() ? "" : used.get(used.size() - 1);
            available =
                    fitting.stream().filter(frame -> !frame.id().equals(latest)).toList();
        }
        if (available.isEmpty()) {
            available = fitting;
        }
        return available.get(random.nextInt(available.size()));
    }

    private static boolean needsOther(EveningFrame frame) {
        return frame.text().contains("{other}");
    }

    private String render(EveningFrame frame, ChatPerson target, ChatPerson other) {
        String text = frame.text().replace("{user}", mention(target));
        if (other != null) {
            text = text.replace("{other}", mention(other));
        }
        if (text.contains("{time}")) {
            String time = catalog.times().get(random.nextInt(catalog.times().size()));
            text = text.replace("{time}", time);
        }
        return text;
    }

    private static String mention(ChatPerson person) {
        if (person.username() != null && USERNAME.matcher(person.username()).matches()) {
            return "@" + person.username();
        }
        return "<a href=\"tg://user?id=" + person.telegramUserId() + "\">" + escape(person.displayName()) + "</a>";
    }

    private static String escape(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
