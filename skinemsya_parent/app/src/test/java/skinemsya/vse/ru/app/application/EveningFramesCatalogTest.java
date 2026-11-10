package skinemsya.vse.ru.app.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class EveningFramesCatalogTest {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{(\\w+)}");
    private static final Set<String> THEMES = Set.of("alcohol", "food", "bill", "night", "company");
    private static final Set<String> SPICES = Set.of("mild", "harsh", "adult");
    private static final Set<String> PLACEHOLDERS = Set.of("user", "other", "time");

    @Test
    void eveningFramesAreUniquePredictionsAboutSomeone() throws Exception {
        JsonNode root;
        try (var in = getClass().getResourceAsStream("/predictions/evening-frames.json")) {
            assertThat(in).isNotNull();
            root = new ObjectMapper().readTree(in);
        }

        assertThat(root.path("times")).hasSizeGreaterThanOrEqualTo(8);

        JsonNode frames = root.path("frames");
        assertThat(frames).hasSizeGreaterThanOrEqualTo(200);

        Set<String> ids = new HashSet<>();
        Set<String> texts = new HashSet<>();
        int adult = 0;
        int pairs = 0;
        for (JsonNode frame : frames) {
            String id = frame.path("id").asText();
            String text = frame.path("text").asText();
            String spice = frame.path("spice").asText();
            assertThat(id).matches("[a-z0-9-]{3,40}");
            assertThat(ids.add(id)).isTrue();
            assertThat(texts.add(text)).isTrue();
            assertThat(THEMES).contains(frame.path("theme").asText());
            assertThat(SPICES).contains(spice);
            if ("adult".equals(spice)) {
                adult++;
            }
            assertThat(text).contains("{user}");
            if (text.contains("{other}")) {
                pairs++;
            }
            assertThat(text).doesNotContain("<", ">", "&");
            assertThat(text.length()).isBetween(40, 400);

            var matcher = PLACEHOLDER.matcher(text);
            while (matcher.find()) {
                assertThat(PLACEHOLDERS).contains(matcher.group(1));
            }
        }
        assertThat(adult * 100).isGreaterThanOrEqualTo(frames.size() * 40);
        assertThat(pairs).isGreaterThanOrEqualTo(24);
    }
}
