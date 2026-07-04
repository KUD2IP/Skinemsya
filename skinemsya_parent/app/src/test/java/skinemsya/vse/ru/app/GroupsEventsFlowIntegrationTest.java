package skinemsya.vse.ru.app;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import skinemsya.vse.ru.app.testsupport.TelegramInitDataTestHelper;

import java.time.Instant;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static skinemsya.vse.ru.app.testsupport.IntegrationTestSupport.authenticate;
import static skinemsya.vse.ru.app.testsupport.IntegrationTestSupport.escapeJson;
import static skinemsya.vse.ru.app.testsupport.IntegrationTestSupport.fetchUserId;
import static skinemsya.vse.ru.app.testsupport.IntegrationTestSupport.readJsonNumberField;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class GroupsEventsFlowIntegrationTest {

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
    void shouldCreateEventInStandaloneGroup() throws Exception {
        var token = authenticate(mockMvc, 200_001L, "Alice");
        var userId = fetchUserId(mockMvc, token);

        var groupResponse = mockMvc.perform(post("/api/v1/groups/standalone")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Friends\"}"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        long groupId = Long.parseLong(readJsonNumberField(groupResponse, "id"));

        mockMvc.perform(post("/api/v1/groups/" + groupId + "/events")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Dinner\",\"description\":\"Pizza\",\"payerId\":" + userId + "}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.payerId").value(userId))
                .andExpect(jsonPath("$.name").value("Dinner"));
    }

    @Test
    void shouldCreateEventInChatLinkedGroup() throws Exception {
        var token = authenticate(mockMvc, 200_002L, "Bob");
        var userId = fetchUserId(mockMvc, token);
        var initData = TelegramInitDataTestHelper.buildInitDataWithChat(
                200_002L, "Bob", Instant.now(), -200_100L, "Trip", "supergroup"
        );

        var groupResponse = mockMvc.perform(post("/api/v1/groups/chat-linked")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"initData\":\"" + escapeJson(initData) + "\"}"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        long groupId = Long.parseLong(readJsonNumberField(groupResponse, "id"));

        mockMvc.perform(post("/api/v1/groups/" + groupId + "/events")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Hotel\",\"payerId\":" + userId + "}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.groupId").value(groupId));
    }

    @Test
    void shouldRejectEventCreationForNonMember() throws Exception {
        var ownerToken = authenticate(mockMvc, 200_003L, "Owner");
        var ownerId = fetchUserId(mockMvc, ownerToken);
        var outsiderToken = authenticate(mockMvc, 200_004L, "Outsider");

        var groupResponse = mockMvc.perform(post("/api/v1/groups/standalone")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Secret\"}"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        long groupId = Long.parseLong(readJsonNumberField(groupResponse, "id"));

        mockMvc.perform(post("/api/v1/groups/" + groupId + "/events")
                        .header("Authorization", "Bearer " + outsiderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Hack\",\"payerId\":" + ownerId + "}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTHORIZATION_ERROR"));
    }

    @Test
    void shouldRejectEventWhenPayerNotGroupMember() throws Exception {
        var ownerToken = authenticate(mockMvc, 200_005L, "Owner");
        var strangerToken = authenticate(mockMvc, 200_006L, "Stranger");
        var strangerId = fetchUserId(mockMvc, strangerToken);

        var groupResponse = mockMvc.perform(post("/api/v1/groups/standalone")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Trip\"}"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        long groupId = Long.parseLong(readJsonNumberField(groupResponse, "id"));

        mockMvc.perform(post("/api/v1/groups/" + groupId + "/events")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Tickets\",\"payerId\":" + strangerId + "}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("DOMAIN_RULE_VIOLATION"));
    }

    @Test
    void shouldListEventsForGroupMember() throws Exception {
        var token = authenticate(mockMvc, 200_007L, "Lister");
        var userId = fetchUserId(mockMvc, token);

        var groupResponse = mockMvc.perform(post("/api/v1/groups/standalone")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Party\"}"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        long groupId = Long.parseLong(readJsonNumberField(groupResponse, "id"));

        mockMvc.perform(post("/api/v1/groups/" + groupId + "/events")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Snacks\",\"payerId\":" + userId + "}"))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/groups/" + groupId + "/events")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].name").value("Snacks"));
    }

    @Test
    void shouldAddLateJoinerToActiveDraftEvent() throws Exception {
        var ownerToken = authenticate(mockMvc, 200_008L, "OwnerLate");
        var ownerId = fetchUserId(mockMvc, ownerToken);

        var groupResponse = mockMvc.perform(post("/api/v1/groups/standalone")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Late join\"}"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        long groupId = Long.parseLong(readJsonNumberField(groupResponse, "id"));

        var eventResponse = mockMvc.perform(post("/api/v1/groups/" + groupId + "/events")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Open collection\",\"payerId\":" + ownerId + "}"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        long eventId = Long.parseLong(readJsonNumberField(eventResponse, "id"));

        var lateToken = authenticate(mockMvc, 200_009L, "LateUser", "lateuser");
        mockMvc.perform(post("/api/v1/groups/" + groupId + "/members")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"telegramUsername\":\"lateuser\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/events/" + eventId + "/participants-status")
                        .header("Authorization", "Bearer " + lateToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalParticipants").value(2));
    }

    @Test
    void shouldLaunchSoloEventWithSingleParticipant() throws Exception {
        var ownerToken = authenticate(mockMvc, 200_010L, "SoloOwner");
        var ownerId = fetchUserId(mockMvc, ownerToken);

        mockMvc.perform(put("/api/v1/users/me/profile")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"paymentDetails\":\"Card solo\"}"))
                .andExpect(status().isOk());

        var groupResponse = mockMvc.perform(post("/api/v1/groups/standalone")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Solo group\"}"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        long groupId = Long.parseLong(readJsonNumberField(groupResponse, "id"));

        var eventResponse = mockMvc.perform(post("/api/v1/groups/" + groupId + "/events")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Solo event\",\"payerId\":" + ownerId + "}"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        long eventId = Long.parseLong(readJsonNumberField(eventResponse, "id"));

        mockMvc.perform(post("/api/v1/events/" + eventId + "/positions")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Coffee\",\"quantity\":1,\"totalPriceKopecks\":30000}"))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/events/" + eventId + "/send-to-distribution")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DISTRIBUTION"));
    }
}
