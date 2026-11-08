package skinemsya.vse.ru.integrations.infrastructure.telegram;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import skinemsya.vse.ru.common.invite.InviteCodes;

class TelegramStartParamTest {

    private static final String CODE = "0123456789abcdef0123456789abcdef";

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
    void shouldAcceptOnlyUnguessableInviteCodes() {
        assertThat(InviteCodes.isToken(CODE)).isTrue();
        assertThat(TelegramStartParam.forEventCode(CODE)).isEqualTo("event_" + CODE);
        assertThat(TelegramStartParam.parseEventCode("event_" + CODE)).contains(CODE);
        assertThat(TelegramStartParam.forGroupCode(CODE)).isEqualTo("group_" + CODE);
        assertThat(TelegramStartParam.parseGroupCode("group_" + CODE)).contains(CODE);
        assertThat(TelegramStartParam.parseEventCode("event_h2a")).isEmpty();
        assertThat(TelegramStartParam.parseEventCode("event_42")).isEmpty();
        assertThat(TelegramStartParam.parseGroupCode("group_h11")).isEmpty();
        assertThat(TelegramStartParam.parseGroupCode("group_17")).isEmpty();
    }
}
