package skinemsya.vse.ru.integrations.infrastructure.telegram;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TelegramStartParamTest {

    @Test
    void shouldEncodeAndDecodeChatId() {
        assertThat(TelegramStartParam.forChat(-1_001_234_567_890L)).isEqualTo("chat_-1001234567890");
        assertThat(TelegramStartParam.parseChatId("chat_-1001234567890")).contains(-1_001_234_567_890L);
    }

    @Test
    void shouldIgnoreUnknownStartParam() {
        assertThat(TelegramStartParam.parseChatId("invite_abc")).isEmpty();
    }

    @Test
    void shouldEncodeAndDecodeEventId() {
        assertThat(TelegramStartParam.forEvent(42L)).isEqualTo("event_42");
        assertThat(TelegramStartParam.parseEventId("event_42")).contains(42L);
    }
}
