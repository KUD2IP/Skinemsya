package skinemsya.vse.ru.app;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static skinemsya.vse.ru.app.testsupport.IntegrationTestSupport.authenticate;
import static skinemsya.vse.ru.app.testsupport.IntegrationTestSupport.fetchUserId;
import static skinemsya.vse.ru.app.testsupport.IntegrationTestSupport.readJsonNumberField;

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

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class SelectionFlowIntegrationTest {

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
    void shouldExposeSelectedByAfterCompleteAndAllowReopenWhileUnpaid() throws Exception {
        var context = prepareDistributedEvent(500_001L, 500_002L, "payer500", "debtor500");

        mockMvc.perform(put("/api/v1/events/" + context.eventId() + "/selections")
                        .header("Authorization", "Bearer " + context.debtorToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"selections\":[{\"positionId\":" + context.positionId() + ",\"quantity\":1}]}"))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/v1/events/" + context.eventId() + "/complete-selection")
                        .header("Authorization", "Bearer " + context.debtorToken()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/events/" + context.eventId() + "/positions")
                        .header("Authorization", "Bearer " + context.payerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].selectedBy[0].userId").value((int) context.debtorId()))
                .andExpect(jsonPath("$[0].selectedBy[0].quantity").value(1));

        mockMvc.perform(get("/api/v1/events/" + context.eventId() + "/positions")
                        .header("Authorization", "Bearer " + context.debtorToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].mySelectedQuantity").value(1));

        mockMvc.perform(post("/api/v1/events/" + context.eventId() + "/reopen-selection")
                        .header("Authorization", "Bearer " + context.debtorToken()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/events/" + context.eventId() + "/participants-status")
                        .header("Authorization", "Bearer " + context.debtorToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completedSelections").value(0));
    }

    @Test
    void shouldReopenSelectionAfterCalculatedWhenAllDebtsUnpaid() throws Exception {
        var context = prepareDistributedEvent(500_011L, 500_012L, "payer501", "debtor501");

        mockMvc.perform(put("/api/v1/events/" + context.eventId() + "/selections")
                        .header("Authorization", "Bearer " + context.debtorToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"selections\":[{\"positionId\":" + context.positionId() + ",\"quantity\":1}]}"))
                .andExpect(status().isNoContent());
        mockMvc.perform(post("/api/v1/events/" + context.eventId() + "/complete-selection")
                        .header("Authorization", "Bearer " + context.debtorToken()))
                .andExpect(status().isNoContent());
        mockMvc.perform(post("/api/v1/events/" + context.eventId() + "/complete-selection")
                        .header("Authorization", "Bearer " + context.payerToken()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/events/" + context.eventId())
                        .header("Authorization", "Bearer " + context.payerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CALCULATED"));

        mockMvc.perform(get("/api/v1/events/" + context.eventId() + "/positions")
                        .header("Authorization", "Bearer " + context.debtorToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].mySelectedQuantity").value(1))
                .andExpect(jsonPath("$[0].selectedBy[0].userId").value((int) context.debtorId()));

        mockMvc.perform(post("/api/v1/events/" + context.eventId() + "/reopen-selection")
                        .header("Authorization", "Bearer " + context.debtorToken()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/events/" + context.eventId())
                        .header("Authorization", "Bearer " + context.debtorToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DISTRIBUTION"));

        mockMvc.perform(put("/api/v1/events/" + context.eventId() + "/selections")
                        .header("Authorization", "Bearer " + context.debtorToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"selections\":[{\"positionId\":" + context.positionId() + ",\"quantity\":2}]}"))
                .andExpect(status().isNoContent());
        mockMvc.perform(post("/api/v1/events/" + context.eventId() + "/complete-selection")
                        .header("Authorization", "Bearer " + context.debtorToken()))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldRejectReopenAfterDebtorConfirmed() throws Exception {
        var context = prepareDistributedEvent(500_021L, 500_022L, "payer502", "debtor502");

        mockMvc.perform(put("/api/v1/events/" + context.eventId() + "/selections")
                        .header("Authorization", "Bearer " + context.debtorToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"selections\":[{\"positionId\":" + context.positionId() + ",\"quantity\":1}]}"))
                .andExpect(status().isNoContent());
        mockMvc.perform(post("/api/v1/events/" + context.eventId() + "/complete-selection")
                        .header("Authorization", "Bearer " + context.debtorToken()))
                .andExpect(status().isNoContent());
        mockMvc.perform(post("/api/v1/events/" + context.eventId() + "/complete-selection")
                        .header("Authorization", "Bearer " + context.payerToken()))
                .andExpect(status().isNoContent());

        var debtsResponse = mockMvc.perform(get("/api/v1/events/" + context.eventId() + "/debts")
                        .header("Authorization", "Bearer " + context.debtorToken()))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        long debtId = Long.parseLong(readJsonNumberField(debtsResponse.substring(debtsResponse.indexOf('[')), "id"));

        var screenshotResponse = mockMvc.perform(multipart("/api/v1/files")
                        .file(new MockMultipartFile("file", "pay.pdf", "application/pdf", new byte[] {9, 9, 9}))
                        .param("purpose", "payment-proof")
                        .header("Authorization", "Bearer " + context.debtorToken()))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        long screenshotId = Long.parseLong(readJsonNumberField(screenshotResponse, "id"));

        mockMvc.perform(post("/api/v1/debts/" + debtId + "/payment/confirm-debtor")
                        .header("Authorization", "Bearer " + context.debtorToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"screenshotFileId\":" + screenshotId + "}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/events/" + context.eventId() + "/reopen-selection")
                        .header("Authorization", "Bearer " + context.debtorToken()))
                .andExpect(status().isConflict());
    }

    @Test
    void shouldCompleteWhenUnselectedPositionStaysWithPayer() throws Exception {
        var payerToken = authenticate(mockMvc, 500_031L, "Payer", "payer503");
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

        var debtorToken = authenticate(mockMvc, 500_032L, "Debtor", "debtor503");
        var debtorId = fetchUserId(mockMvc, debtorToken);
        mockMvc.perform(post("/api/v1/groups/" + groupId + "/members")
                        .header("Authorization", "Bearer " + payerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"telegramUsername\":\"debtor503\"}"))
                .andExpect(status().isCreated());

        var eventResponse = mockMvc.perform(post("/api/v1/groups/" + groupId + "/events")
                        .header("Authorization", "Bearer " + payerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Drinks\",\"payerId\":" + payerId + ",\"expectedParticipantCount\":2}"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        long eventId = Long.parseLong(readJsonNumberField(eventResponse, "id"));
        mockMvc.perform(post("/api/v1/events/" + eventId + "/join")
                        .header("Authorization", "Bearer " + debtorToken))
                .andExpect(status().isOk());

        var claimedResponse = mockMvc.perform(post("/api/v1/events/" + eventId + "/positions")
                        .header("Authorization", "Bearer " + payerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Salad\",\"quantity\":1,\"totalPriceKopecks\":20000}"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        long claimedPositionId = Long.parseLong(readJsonNumberField(claimedResponse, "id"));
        mockMvc.perform(post("/api/v1/events/" + eventId + "/positions")
                        .header("Authorization", "Bearer " + payerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Wine\",\"quantity\":1,\"totalPriceKopecks\":40000}"))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/events/" + eventId + "/send-to-distribution")
                        .header("Authorization", "Bearer " + payerToken))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/v1/events/" + eventId + "/selections")
                        .header("Authorization", "Bearer " + debtorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"selections\":[{\"positionId\":" + claimedPositionId + ",\"quantity\":1}]}"))
                .andExpect(status().isNoContent());
        mockMvc.perform(post("/api/v1/events/" + eventId + "/complete-selection")
                        .header("Authorization", "Bearer " + debtorToken))
                .andExpect(status().isNoContent());
        mockMvc.perform(post("/api/v1/events/" + eventId + "/complete-selection")
                        .header("Authorization", "Bearer " + payerToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/events/" + eventId)
                        .header("Authorization", "Bearer " + payerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CALCULATED"));

        mockMvc.perform(get("/api/v1/events/" + eventId + "/debts")
                        .header("Authorization", "Bearer " + debtorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].debtorId").value((int) debtorId))
                .andExpect(jsonPath("$[0].amountKopecks").value(20000));
    }

    @Test
    void shouldCompleteLeftoverWithSharedAndAllowPayerToLeaveEdit() throws Exception {
        var payerToken = authenticate(mockMvc, 500_051L, "Payer", "payer505");
        var payerId = fetchUserId(mockMvc, payerToken);
        mockMvc.perform(put("/api/v1/users/me/profile")
                        .header("Authorization", "Bearer " + payerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"paymentDetails\":\"Card 4242\"}"))
                .andExpect(status().isOk());

        var groupResponse = mockMvc.perform(post("/api/v1/groups/standalone")
                        .header("Authorization", "Bearer " + payerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Cafe\"}"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        long groupId = Long.parseLong(readJsonNumberField(groupResponse, "id"));

        var debtorToken = authenticate(mockMvc, 500_052L, "Debtor", "debtor505");
        mockMvc.perform(post("/api/v1/groups/" + groupId + "/members")
                        .header("Authorization", "Bearer " + payerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"telegramUsername\":\"debtor505\"}"))
                .andExpect(status().isCreated());

        var eventResponse = mockMvc.perform(post("/api/v1/groups/" + groupId + "/events")
                        .header("Authorization", "Bearer " + payerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Brunch\",\"payerId\":" + payerId + ",\"expectedParticipantCount\":2}"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        long eventId = Long.parseLong(readJsonNumberField(eventResponse, "id"));
        mockMvc.perform(post("/api/v1/events/" + eventId + "/join")
                        .header("Authorization", "Bearer " + debtorToken))
                .andExpect(status().isOk());

        var teaResponse = mockMvc.perform(post("/api/v1/events/" + eventId + "/positions")
                        .header("Authorization", "Bearer " + payerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Tea\",\"quantity\":1,\"totalPriceKopecks\":10000}"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        long teaId = Long.parseLong(readJsonNumberField(teaResponse, "id"));
        mockMvc.perform(post("/api/v1/events/" + eventId + "/positions")
                        .header("Authorization", "Bearer " + payerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Coffee\",\"quantity\":1,\"totalPriceKopecks\":15000}"))
                .andExpect(status().isCreated());
        var sharedResponse = mockMvc.perform(post("/api/v1/events/" + eventId + "/positions")
                        .header("Authorization", "Bearer " + payerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Cake\",\"quantity\":1,\"totalPriceKopecks\":20000}"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        long sharedId = Long.parseLong(readJsonNumberField(sharedResponse, "id"));
        mockMvc.perform(post("/api/v1/positions/" + sharedId + "/mark-shared")
                        .header("Authorization", "Bearer " + payerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"forAll\":true}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/events/" + eventId + "/send-to-distribution")
                        .header("Authorization", "Bearer " + payerToken))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/v1/events/" + eventId + "/selections")
                        .header("Authorization", "Bearer " + payerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"selections\":[{\"positionId\":" + teaId + ",\"quantity\":1}]}"))
                .andExpect(status().isNoContent());
        mockMvc.perform(post("/api/v1/events/" + eventId + "/complete-selection")
                        .header("Authorization", "Bearer " + payerToken))
                .andExpect(status().isNoContent());
        mockMvc.perform(post("/api/v1/events/" + eventId + "/complete-selection")
                        .header("Authorization", "Bearer " + debtorToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/events/" + eventId)
                        .header("Authorization", "Bearer " + payerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CALCULATED"));
        mockMvc.perform(get("/api/v1/events/" + eventId + "/debts")
                        .header("Authorization", "Bearer " + debtorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].amountKopecks").value(10000));

        mockMvc.perform(post("/api/v1/events/" + eventId + "/reopen-selection")
                        .header("Authorization", "Bearer " + payerToken))
                .andExpect(status().isNoContent());
        mockMvc.perform(put("/api/v1/events/" + eventId + "/selections")
                        .header("Authorization", "Bearer " + payerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"selections\":[{\"positionId\":" + teaId + ",\"quantity\":1}]}"))
                .andExpect(status().isNoContent());
        mockMvc.perform(post("/api/v1/events/" + eventId + "/complete-selection")
                        .header("Authorization", "Bearer " + payerToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/events/" + eventId)
                        .header("Authorization", "Bearer " + payerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CALCULATED"));
    }

    @Test
    void shouldPreviewSameAmountWhenFirstPersonCompletesPartialQuantity() throws Exception {
        var context = prepareDistributedEvent(500_061L, 500_062L, "payer506", "debtor506");

        mockMvc.perform(put("/api/v1/events/" + context.eventId() + "/selections")
                        .header("Authorization", "Bearer " + context.debtorToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"selections\":[{\"positionId\":" + context.positionId() + ",\"quantity\":1}]}"))
                .andExpect(status().isNoContent());
        mockMvc.perform(post("/api/v1/events/" + context.eventId() + "/complete-selection")
                        .header("Authorization", "Bearer " + context.debtorToken()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/events/" + context.eventId() + "/debts")
                        .header("Authorization", "Bearer " + context.debtorToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].amountKopecks").value(30000));
    }

    @Test
    void shouldChargeOnlySelectedShareWhenQuantityIsPartial() throws Exception {
        var payerToken = authenticate(mockMvc, 500_041L, "Payer", "payer504");
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

        var debtorOneToken = authenticate(mockMvc, 500_042L, "DebtorOne", "debtor504a");
        var debtorTwoToken = authenticate(mockMvc, 500_043L, "DebtorTwo", "debtor504b");
        mockMvc.perform(post("/api/v1/groups/" + groupId + "/members")
                        .header("Authorization", "Bearer " + payerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"telegramUsername\":\"debtor504a\"}"))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/v1/groups/" + groupId + "/members")
                        .header("Authorization", "Bearer " + payerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"telegramUsername\":\"debtor504b\"}"))
                .andExpect(status().isCreated());

        var eventResponse = mockMvc.perform(post("/api/v1/groups/" + groupId + "/events")
                        .header("Authorization", "Bearer " + payerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Drinks\",\"payerId\":" + payerId + ",\"expectedParticipantCount\":3}"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        long eventId = Long.parseLong(readJsonNumberField(eventResponse, "id"));
        mockMvc.perform(post("/api/v1/events/" + eventId + "/join")
                        .header("Authorization", "Bearer " + debtorOneToken))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/events/" + eventId + "/join")
                        .header("Authorization", "Bearer " + debtorTwoToken))
                .andExpect(status().isOk());

        var positionResponse = mockMvc.perform(post("/api/v1/events/" + eventId + "/positions")
                        .header("Authorization", "Bearer " + payerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Beer\",\"quantity\":3,\"totalPriceKopecks\":30000}"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        long positionId = Long.parseLong(readJsonNumberField(positionResponse, "id"));

        mockMvc.perform(post("/api/v1/events/" + eventId + "/send-to-distribution")
                        .header("Authorization", "Bearer " + payerToken))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/v1/events/" + eventId + "/selections")
                        .header("Authorization", "Bearer " + debtorOneToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"selections\":[{\"positionId\":" + positionId + ",\"quantity\":1}]}"))
                .andExpect(status().isNoContent());
        mockMvc.perform(put("/api/v1/events/" + eventId + "/selections")
                        .header("Authorization", "Bearer " + debtorTwoToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"selections\":[{\"positionId\":" + positionId + ",\"quantity\":1}]}"))
                .andExpect(status().isNoContent());
        mockMvc.perform(post("/api/v1/events/" + eventId + "/complete-selection")
                        .header("Authorization", "Bearer " + debtorOneToken))
                .andExpect(status().isNoContent());
        mockMvc.perform(post("/api/v1/events/" + eventId + "/complete-selection")
                        .header("Authorization", "Bearer " + debtorTwoToken))
                .andExpect(status().isNoContent());
        mockMvc.perform(post("/api/v1/events/" + eventId + "/complete-selection")
                        .header("Authorization", "Bearer " + payerToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/events/" + eventId)
                        .header("Authorization", "Bearer " + payerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CALCULATED"));

        mockMvc.perform(get("/api/v1/events/" + eventId + "/debts")
                        .header("Authorization", "Bearer " + payerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].amountKopecks").value(10000))
                .andExpect(jsonPath("$[1].amountKopecks").value(10000));
    }

    private PreparedSelectionContext prepareDistributedEvent(
            long payerTelegramId, long debtorTelegramId, String payerUsername, String debtorUsername)
            throws Exception {
        var payerToken = authenticate(mockMvc, payerTelegramId, "Payer", payerUsername);
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

        var debtorToken = authenticate(mockMvc, debtorTelegramId, "Debtor", debtorUsername);
        var debtorId = fetchUserId(mockMvc, debtorToken);
        mockMvc.perform(post("/api/v1/groups/" + groupId + "/members")
                        .header("Authorization", "Bearer " + payerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"telegramUsername\":\"" + debtorUsername + "\"}"))
                .andExpect(status().isCreated());

        var eventResponse = mockMvc.perform(post("/api/v1/groups/" + groupId + "/events")
                        .header("Authorization", "Bearer " + payerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Drinks\",\"payerId\":" + payerId + ",\"expectedParticipantCount\":4}"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        long eventId = Long.parseLong(readJsonNumberField(eventResponse, "id"));
        mockMvc.perform(post("/api/v1/events/" + eventId + "/join")
                        .header("Authorization", "Bearer " + debtorToken))
                .andExpect(status().isOk());

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

        return new PreparedSelectionContext(eventId, positionId, debtorId, payerToken, debtorToken);
    }

    private record PreparedSelectionContext(
            long eventId, long positionId, long debtorId, String payerToken, String debtorToken) {}
}
