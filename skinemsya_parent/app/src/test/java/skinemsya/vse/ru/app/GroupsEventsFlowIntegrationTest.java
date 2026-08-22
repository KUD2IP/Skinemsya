package skinemsya.vse.ru.app;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static skinemsya.vse.ru.app.testsupport.IntegrationTestSupport.authenticate;
import static skinemsya.vse.ru.app.testsupport.IntegrationTestSupport.escapeJson;
import static skinemsya.vse.ru.app.testsupport.IntegrationTestSupport.fetchUserId;
import static skinemsya.vse.ru.app.testsupport.IntegrationTestSupport.readJsonNumberField;

import java.time.Instant;
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

        var eventResponse = mockMvc.perform(post("/api/v1/groups/" + groupId + "/events")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Dinner\",\"description\":\"Pizza\",\"payerId\":" + userId + ",\"expectedParticipantCount\":4}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.payerId").value(userId))
                .andExpect(jsonPath("$.name").value("Dinner"))
                .andExpect(jsonPath("$.expectedParticipantCount").value(4))
                .andExpect(jsonPath("$.joinedCount").value(1))
                .andExpect(jsonPath("$.currentUserJoined").value(true))
                .andReturn()
                .getResponse()
                .getContentAsString();
        long eventId = Long.parseLong(readJsonNumberField(eventResponse, "id"));

        mockMvc.perform(get("/api/v1/events/" + eventId + "/invite-link").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.startParam").value("event_" + eventId))
                .andExpect(jsonPath("$.shareText").value(
                        "Присоединяйся к сбору «Dinner» в группе «Friends» в Skinemsya — делим расходы вместе:"));
    }

    @Test
    void shouldCreateEventInChatLinkedGroup() throws Exception {
        var token = authenticate(mockMvc, 200_002L, "Bob");
        var userId = fetchUserId(mockMvc, token);
        var initData = TelegramInitDataTestHelper.buildInitDataWithChat(
                200_002L, "Bob", Instant.now(), -200_100L, "Trip", "supergroup");

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
                        .content("{\"name\":\"Hotel\",\"payerId\":" + userId + ",\"expectedParticipantCount\":4}"))
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
                        .content("{\"name\":\"Hack\",\"payerId\":" + ownerId + ",\"expectedParticipantCount\":4}"))
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
                        .content("{\"name\":\"Tickets\",\"payerId\":" + strangerId + ",\"expectedParticipantCount\":4}"))
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
                        .content("{\"name\":\"Snacks\",\"payerId\":" + userId + ",\"expectedParticipantCount\":4}"))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/groups/" + groupId + "/events").header("Authorization", "Bearer " + token))
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
                        .content("{\"name\":\"Open collection\",\"payerId\":" + ownerId + ",\"expectedParticipantCount\":4}"))
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

        mockMvc.perform(get("/api/v1/events/" + eventId)
                        .header("Authorization", "Bearer " + lateToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.joinedCount").value(1))
                .andExpect(jsonPath("$.expectedParticipantCount").value(4))
                .andExpect(jsonPath("$.currentUserJoined").value(false));

        mockMvc.perform(post("/api/v1/events/" + eventId + "/join")
                        .header("Authorization", "Bearer " + lateToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.joinedCount").value(2))
                .andExpect(jsonPath("$.currentUserJoined").value(true));

        mockMvc.perform(get("/api/v1/events/" + eventId + "/participants-status")
                        .header("Authorization", "Bearer " + lateToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalParticipants").value(2))
                .andExpect(jsonPath("$.expectedParticipantCount").value(4))
                .andExpect(jsonPath("$.joinedCount").value(2));

        mockMvc.perform(post("/api/v1/events/" + eventId + "/leave")
                        .header("Authorization", "Bearer " + lateToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.joinedCount").value(1))
                .andExpect(jsonPath("$.currentUserJoined").value(false));
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
                        .content("{\"name\":\"Solo event\",\"payerId\":" + ownerId + ",\"expectedParticipantCount\":4}"))
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

    @Test
    void shouldDeleteNonDraftEventAndRemoveParticipant() throws Exception {
        var ownerToken = authenticate(mockMvc, 200_011L, "OwnerKick");
        var ownerId = fetchUserId(mockMvc, ownerToken);
        var memberToken = authenticate(mockMvc, 200_012L, "MemberKick", "memberkick");
        var memberId = fetchUserId(mockMvc, memberToken);

        var groupResponse = mockMvc.perform(post("/api/v1/groups/standalone")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Kick group\"}"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        long groupId = Long.parseLong(readJsonNumberField(groupResponse, "id"));

        mockMvc.perform(post("/api/v1/groups/" + groupId + "/members")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"telegramUsername\":\"memberkick\"}"))
                .andExpect(status().isCreated());

        var eventResponse = mockMvc.perform(post("/api/v1/groups/" + groupId + "/events")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Kick event\",\"payerId\":" + ownerId + ",\"expectedParticipantCount\":4}"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        long eventId = Long.parseLong(readJsonNumberField(eventResponse, "id"));

        mockMvc.perform(post("/api/v1/events/" + eventId + "/join")
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.joinedCount").value(2));

        mockMvc.perform(delete("/api/v1/events/" + eventId + "/participants/" + memberId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.joinedCount").value(1));

        mockMvc.perform(post("/api/v1/events/" + eventId + "/join")
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/v1/groups/" + groupId + "/members/" + memberId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/events/" + eventId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.joinedCount").value(1));

        mockMvc.perform(delete("/api/v1/events/" + eventId).header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/events/" + eventId).header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldRejectRemovingPayerFromGroup() throws Exception {
        var ownerToken = authenticate(mockMvc, 200_013L, "OwnerPayer");
        var memberToken = authenticate(mockMvc, 200_014L, "PayerMember", "payermember");
        var memberId = fetchUserId(mockMvc, memberToken);

        var groupResponse = mockMvc.perform(post("/api/v1/groups/standalone")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Payer lock\"}"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        long groupId = Long.parseLong(readJsonNumberField(groupResponse, "id"));

        mockMvc.perform(post("/api/v1/groups/" + groupId + "/members")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"telegramUsername\":\"payermember\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/groups/" + groupId + "/events")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Paid by member\",\"payerId\":" + memberId + ",\"expectedParticipantCount\":4}"))
                .andExpect(status().isCreated());

        mockMvc.perform(delete("/api/v1/groups/" + groupId + "/members/" + memberId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("DOMAIN_RULE_VIOLATION"));
    }
}
