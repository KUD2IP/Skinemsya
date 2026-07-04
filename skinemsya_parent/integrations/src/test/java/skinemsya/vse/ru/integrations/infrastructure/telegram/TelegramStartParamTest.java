package skinemsya.vse.ru.integrations.infrastructure.telegram;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TelegramStartParamTest {

    @Test
    void shouldEncodeAndDecodeChatId() {
        assertThat(TelegramStartParam.forChat(-1_001_234_567_890L)).isEqualTo("chat_-1001234567890");
        assertThat(TelegramStartParam.parseChatId("chat_-1001234567890"))
                .contains(-1_001_234_567_890L);
    }

    @Test
    void shouldIgnoreUnknownStartParam() {
        assertThat(TelegramStartParam.parseChatId("invite_abc")).isEmpty();
    }
}
