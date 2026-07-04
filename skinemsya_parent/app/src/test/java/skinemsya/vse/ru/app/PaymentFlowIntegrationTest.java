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
class PaymentFlowIntegrationTest {

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
    void shouldCompletePaymentFlowAndCloseEvent() throws Exception {
        var payerToken = authenticate(mockMvc, 400_001L, "Payer", "payer400");
        var payerId = fetchUserId(mockMvc, payerToken);

        mockMvc.perform(put("/api/v1/users/me/profile")
                        .header("Authorization", "Bearer " + payerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"paymentDetails\":\"Card 4242\"}"))
                .andExpect(status().isOk());

        var groupResponse = mockMvc.perform(post("/api/v1/groups/standalone")
                        .header("Authorization", "Bearer " + payerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Bar\"}"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        long groupId = Long.parseLong(readJsonNumberField(groupResponse, "id"));

        var debtorToken = authenticate(mockMvc, 400_002L, "Debtor", "debtor400");
        mockMvc.perform(post("/api/v1/groups/" + groupId + "/members")
                        .header("Authorization", "Bearer " + payerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"telegramUsername\":\"debtor400\"}"))
                .andExpect(status().isCreated());

        var eventResponse = mockMvc.perform(post("/api/v1/groups/" + groupId + "/events")
                        .header("Authorization", "Bearer " + payerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Drinks\",\"payerId\":" + payerId + "}"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        long eventId = Long.parseLong(readJsonNumberField(eventResponse, "id"));

        var positionResponse = mockMvc.perform(post("/api/v1/events/" + eventId + "/positions")
                        .header("Authorization", "Bearer " + payerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Beer\",\"quantity\":2,\"totalPriceKopecks\":60000}"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        long positionId = Long.parseLong(readJsonNumberField(positionResponse, "id"));

        mockMvc.perform(post("/api/v1/events/" + eventId + "/send-to-distribution")
                        .header("Authorization", "Bearer " + payerToken))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/v1/events/" + eventId + "/selections")
                        .header("Authorization", "Bearer " + debtorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"selections\":[{\"positionId\":" + positionId + ",\"quantity\":1}]}"))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/v1/events/" + eventId + "/complete-selection")
                        .header("Authorization", "Bearer " + debtorToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/events/" + eventId + "/debts")
                        .header("Authorization", "Bearer " + debtorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].debtorId").exists())
                .andExpect(jsonPath("$[0].status").value("UNPAID"));

        mockMvc.perform(get("/api/v1/events/" + eventId)
                        .header("Authorization", "Bearer " + debtorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DISTRIBUTION"));

        mockMvc.perform(post("/api/v1/events/" + eventId + "/complete-selection")
                        .header("Authorization", "Bearer " + payerToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/events/" + eventId)
                        .header("Authorization", "Bearer " + payerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CALCULATED"));

        var debtsResponse = mockMvc.perform(get("/api/v1/events/" + eventId + "/debts")
                        .header("Authorization", "Bearer " + debtorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").exists())
                .andReturn()
                .getResponse()
                .getContentAsString();
        long debtId = Long.parseLong(readJsonNumberField(debtsResponse.substring(debtsResponse.indexOf('[')), "id"));

        var screenshotResponse = mockMvc.perform(multipart("/api/v1/files")
                        .file(new MockMultipartFile("file", "pay.pdf", "application/pdf", new byte[]{9, 9, 9}))
                        .param("purpose", "payment-proof")
                        .header("Authorization", "Bearer " + debtorToken))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        long screenshotId = Long.parseLong(readJsonNumberField(screenshotResponse, "id"));

        mockMvc.perform(post("/api/v1/debts/" + debtId + "/payment/confirm-debtor")
                        .header("Authorization", "Bearer " + debtorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"screenshotFileId\":" + screenshotId + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DEBTOR_CONFIRMED"));

        mockMvc.perform(post("/api/v1/events/" + eventId + "/payments/confirm-all")
                        .header("Authorization", "Bearer " + payerToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/events/" + eventId)
                        .header("Authorization", "Bearer " + payerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }
}
