package skinemsya.vse.ru.app;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static skinemsya.vse.ru.app.testsupport.IntegrationTestSupport.authenticate;
import static skinemsya.vse.ru.app.testsupport.IntegrationTestSupport.fetchUserId;
import static skinemsya.vse.ru.app.testsupport.IntegrationTestSupport.readJsonNumberField;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class ReceiptsFlowIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("skinemsya")
            .withUsername("skinemsya")
            .withPassword("skinemsya");

    @DynamicPropertySource
    static void configureDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldAddPositionsAndSendToDistribution() throws Exception {
        var token = authenticate(mockMvc, 300_001L, "Payer");
        var userId = fetchUserId(mockMvc, token);

        var groupResponse = mockMvc.perform(post("/api/v1/groups/standalone")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Dinner\"}"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        long groupId = Long.parseLong(readJsonNumberField(groupResponse, "id"));

        mockMvc.perform(put("/api/v1/users/me/profile")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"paymentDetails\":\"Card 1234\"}"))
                .andExpect(status().isOk());

        var token2 = authenticate(mockMvc, 300_002L, "Friend", "friend2");
        mockMvc.perform(post("/api/v1/groups/" + groupId + "/members")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"telegramUsername\":\"friend2\"}"))
                .andExpect(status().isCreated());

        var eventResponse = mockMvc.perform(post("/api/v1/groups/" + groupId + "/events")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Pizza night\",\"payerId\":" + userId + "}"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        long eventId = Long.parseLong(readJsonNumberField(eventResponse, "id"));

        mockMvc.perform(post("/api/v1/events/" + eventId + "/positions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Margherita\",\"quantity\":1,\"totalPriceKopecks\":50000}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Margherita"));

        var fileResponse = mockMvc.perform(multipart("/api/v1/files")
                        .file(new MockMultipartFile("file", "receipt.png", "image/png", new byte[]{1, 2, 3}))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        long fileId = Long.parseLong(readJsonNumberField(fileResponse, "id"));

        mockMvc.perform(post("/api/v1/events/" + eventId + "/receipts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fileId\":" + fileId + "}"))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/events/" + eventId + "/send-to-distribution")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DISTRIBUTION"));

        mockMvc.perform(post("/api/v1/events/" + eventId + "/send-to-distribution")
                        .header("Authorization", "Bearer " + token2))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void shouldUpdateAndDeletePositionInDraft() throws Exception {
        var token = authenticate(mockMvc, 300_010L, "Editor");
        var userId = fetchUserId(mockMvc, token);

        var groupResponse = mockMvc.perform(post("/api/v1/groups/standalone")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Edit group\"}"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        long groupId = Long.parseLong(readJsonNumberField(groupResponse, "id"));

        var eventResponse = mockMvc.perform(post("/api/v1/groups/" + groupId + "/events")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Edit event\",\"payerId\":" + userId + "}"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        long eventId = Long.parseLong(readJsonNumberField(eventResponse, "id"));

        var positionResponse = mockMvc.perform(post("/api/v1/events/" + eventId + "/positions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Soup\",\"quantity\":1,\"totalPriceKopecks\":30000}"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        long positionId = Long.parseLong(readJsonNumberField(positionResponse, "id"));

        mockMvc.perform(put("/api/v1/positions/" + positionId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Borscht\",\"quantity\":2,\"totalPriceKopecks\":40000}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Borscht"));

        mockMvc.perform(delete("/api/v1/positions/" + positionId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldListReceiptsForEvent() throws Exception {
        var token = authenticate(mockMvc, 300_020L, "ReceiptViewer");
        var userId = fetchUserId(mockMvc, token);

        var groupResponse = mockMvc.perform(post("/api/v1/groups/standalone")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Receipt group\"}"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        long groupId = Long.parseLong(readJsonNumberField(groupResponse, "id"));

        var eventResponse = mockMvc.perform(post("/api/v1/groups/" + groupId + "/events")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Receipt event\",\"payerId\":" + userId + "}"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        long eventId = Long.parseLong(readJsonNumberField(eventResponse, "id"));

        var fileResponse = mockMvc.perform(multipart("/api/v1/files")
                        .file(new MockMultipartFile("file", "receipt.png", "image/png", new byte[]{1, 2, 3}))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        long fileId = Long.parseLong(readJsonNumberField(fileResponse, "id"));

        mockMvc.perform(post("/api/v1/events/" + eventId + "/receipts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fileId\":" + fileId + "}"))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/events/" + eventId + "/receipts")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].fileId").value(fileId));
    }

    @Test
    void shouldRejectOversizedAndInvalidReceiptUploads() throws Exception {
        var token = authenticate(mockMvc, 300_030L, "Uploader");

        mockMvc.perform(multipart("/api/v1/files")
                        .file(new MockMultipartFile("file", "big.png", "image/png", new byte[2048]))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("File exceeds maximum size of 1 MB"));

        mockMvc.perform(multipart("/api/v1/files")
                        .file(new MockMultipartFile("file", "receipt.pdf", "application/pdf", new byte[]{1, 2, 3}))
                        .param("purpose", "receipt")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("Only image files are allowed for receipt upload"));
    }
}
