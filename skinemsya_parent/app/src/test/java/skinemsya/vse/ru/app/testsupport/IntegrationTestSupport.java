package skinemsya.vse.ru.app.testsupport;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

public final class IntegrationTestSupport {

    private IntegrationTestSupport() {}

    public static String authenticate(MockMvc mockMvc, long telegramUserId, String firstName) throws Exception {
        return authenticate(mockMvc, telegramUserId, firstName, null);
    }

    public static String authenticate(MockMvc mockMvc, long telegramUserId, String firstName, String username)
            throws Exception {
        var initData = TelegramInitDataTestHelper.buildInitData(telegramUserId, firstName, Instant.now(), username);
        var response = mockMvc.perform(post("/api/v1/auth/telegram")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"initData\":\"" + escapeJson(initData) + "\"}"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return readJsonField(response, "accessToken");
    }

    public static long fetchUserId(MockMvc mockMvc, String accessToken) throws Exception {
        var response = mockMvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return Long.parseLong(readJsonNumberField(response, "id"));
    }

    public static String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    public static String readJsonField(String json, String field) {
        var marker = "\"" + field + "\":\"";
        int start = json.indexOf(marker);
        if (start < 0) {
            throw new IllegalStateException("Field not found: " + field);
        }
        start += marker.length();
        int end = json.indexOf('"', start);
        return json.substring(start, end);
    }

    public static String readJsonNumberField(String json, String field) {
        var marker = "\"" + field + "\":";
        int start = json.indexOf(marker);
        if (start < 0) {
            throw new IllegalStateException("Field not found: " + field);
        }
        start += marker.length();
        int end = start;
        while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '-')) {
            end++;
        }
        return json.substring(start, end);
    }
}
