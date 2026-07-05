package skinemsya.vse.ru.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static skinemsya.vse.ru.app.testsupport.IntegrationTestSupport.authenticate;
import static skinemsya.vse.ru.app.testsupport.IntegrationTestSupport.escapeJson;
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
import skinemsya.vse.ru.app.testsupport.IntegrationTestSupport;
import skinemsya.vse.ru.app.testsupport.TelegramInitDataTestHelper;
import skinemsya.vse.ru.events.domain.EventStatus;
import skinemsya.vse.ru.events.infrastructure.persistence.EventEntity;
import skinemsya.vse.ru.events.infrastructure.persistence.EventRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class GroupsFlowIntegrationTest {

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

    @Autowired
    private EventRepository eventRepository;

    @Test
    void shouldCreateStandaloneGroupWithOwner() throws Exception {
        var token = authenticate(mockMvc, 100_001L, "Alice");

        mockMvc.perform(post("/api/v1/groups/standalone")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Friends\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Friends"))
                .andExpect(jsonPath("$.type").value("STANDALONE"))
                .andExpect(jsonPath("$.ownerId").isNumber());
    }

    @Test
    void shouldCreateChatLinkedGroupFromSignedInitData() throws Exception {
        var token = authenticate(mockMvc, 100_002L, "Bob");
        var initData = TelegramInitDataTestHelper.buildInitDataWithChat(
                100_002L, "Bob", Instant.now(), -100_500L, "Team chat", "supergroup");

        mockMvc.perform(post("/api/v1/groups/chat-linked")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"initData\":\"" + escapeJson(initData) + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("CHAT_LINKED"))
                .andExpect(jsonPath("$.telegramChatId").value(-100_500))
                .andExpect(jsonPath("$.name").value("Team chat"));
    }

    @Test
    void shouldRejectChatLinkedInitDataForAnotherTelegramUser() throws Exception {
        var token = authenticate(mockMvc, 100_020L, "TokenOwner");
        var initData = TelegramInitDataTestHelper.buildInitDataWithChat(
                100_021L, "OtherUser", Instant.now(), -100_520L, "Other chat", "supergroup");

        mockMvc.perform(post("/api/v1/groups/chat-linked")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"initData\":\"" + escapeJson(initData) + "\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTHORIZATION_ERROR"));
    }

    @Test
    void shouldJoinExistingChatLinkedGroupIdempotently() throws Exception {
        var token = authenticate(mockMvc, 100_003L, "Carol");
        var initData = TelegramInitDataTestHelper.buildInitDataWithChat(
                100_003L, "Carol", Instant.now(), -100_501L, "Weekend", "group");
        var body = "{\"initData\":\"" + escapeJson(initData) + "\"}";

        var first = mockMvc.perform(post("/api/v1/groups/chat-linked")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        var second = mockMvc.perform(post("/api/v1/groups/chat-linked")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(readJsonNumberField(first, "id")).isEqualTo(readJsonNumberField(second, "id"));
    }

    @Test
    void shouldRejectChatLinkedWithoutChatInInitData() throws Exception {
        var token = authenticate(mockMvc, 100_004L, "Dave");
        var initData = TelegramInitDataTestHelper.buildInitData(100_004L, "Dave", Instant.now());

        mockMvc.perform(post("/api/v1/groups/chat-linked")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"initData\":\"" + escapeJson(initData) + "\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("DOMAIN_RULE_VIOLATION"));
    }

    @Test
    void shouldAddMemberToStandaloneGroupAsOwner() throws Exception {
        var ownerToken = authenticate(mockMvc, 100_005L, "Owner", "group_owner");
        authenticate(mockMvc, 100_006L, "Member", "group_member");

        var createResponse = mockMvc.perform(post("/api/v1/groups/standalone")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Roommates\"}"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        long groupId = Long.parseLong(readJsonNumberField(createResponse, "id"));

        mockMvc.perform(post("/api/v1/groups/" + groupId + "/members")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"telegramUsername\":\"group_member\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").isNumber())
                .andExpect(jsonPath("$.role").value("MEMBER"))
                .andExpect(jsonPath("$.telegramUsername").value("group_member"));

        mockMvc.perform(get("/api/v1/groups/" + groupId + "/members").header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(2));
    }

    @Test
    void shouldRenameGroupAsOwner() throws Exception {
        var ownerToken = authenticate(mockMvc, 100_009L, "Renamer");

        var createResponse = mockMvc.perform(post("/api/v1/groups/standalone")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Old name\"}"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        long groupId = Long.parseLong(readJsonNumberField(createResponse, "id"));

        mockMvc.perform(put("/api/v1/groups/" + groupId)
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"New name\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("New name"));

        mockMvc.perform(get("/api/v1/groups/" + groupId).header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("New name"));
    }

    @Test
    void shouldRejectRenameForNonOwner() throws Exception {
        var ownerToken = authenticate(mockMvc, 100_010L, "Owner");
        var memberToken = authenticate(mockMvc, 100_011L, "Member", "rename_member");

        var createResponse = mockMvc.perform(post("/api/v1/groups/standalone")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Shared\"}"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        long groupId = Long.parseLong(readJsonNumberField(createResponse, "id"));

        mockMvc.perform(post("/api/v1/groups/" + groupId + "/members")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"telegramUsername\":\"rename_member\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(put("/api/v1/groups/" + groupId)
                        .header("Authorization", "Bearer " + memberToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Hacked\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTHORIZATION_ERROR"));
    }

    @Test
    void shouldDeleteStandaloneGroupWithoutBlockingEvents() throws Exception {
        var ownerToken = authenticate(mockMvc, 100_012L, "Deleter");

        var createResponse = mockMvc.perform(post("/api/v1/groups/standalone")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Temporary\"}"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        long groupId = Long.parseLong(readJsonNumberField(createResponse, "id"));

        mockMvc.perform(delete("/api/v1/groups/" + groupId).header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/groups/" + groupId).header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldRejectDeleteWhenNonDraftEventExists() throws Exception {
        var ownerToken = authenticate(mockMvc, 100_013L, "Blocked");
        var userId = IntegrationTestSupport.fetchUserId(mockMvc, ownerToken);

        var createResponse = mockMvc.perform(post("/api/v1/groups/standalone")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"With event\"}"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        long groupId = Long.parseLong(readJsonNumberField(createResponse, "id"));

        var eventResponse = mockMvc.perform(post("/api/v1/groups/" + groupId + "/events")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Active\",\"payerId\":" + userId + "}"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        long eventId = Long.parseLong(readJsonNumberField(eventResponse, "id"));

        EventEntity event = eventRepository.findById(eventId).orElseThrow();
        event.setStatus(EventStatus.DISTRIBUTION);
        eventRepository.save(event);

        mockMvc.perform(delete("/api/v1/groups/" + groupId).header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("DOMAIN_RULE_VIOLATION"));
    }

    @Test
    void shouldDeleteGroupAfterDraftEventsCascade() throws Exception {
        var ownerToken = authenticate(mockMvc, 100_014L, "Cascade");
        var userId = IntegrationTestSupport.fetchUserId(mockMvc, ownerToken);

        var createResponse = mockMvc.perform(post("/api/v1/groups/standalone")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Drafts only\"}"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        long groupId = Long.parseLong(readJsonNumberField(createResponse, "id"));

        mockMvc.perform(post("/api/v1/groups/" + groupId + "/events")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Draft\",\"payerId\":" + userId + "}"))
                .andExpect(status().isCreated());

        mockMvc.perform(delete("/api/v1/groups/" + groupId).header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/groups/" + groupId).header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldRejectGroupAccessForNonMember() throws Exception {
        var ownerToken = authenticate(mockMvc, 100_007L, "Owner");
        var outsiderToken = authenticate(mockMvc, 100_008L, "Outsider");

        var createResponse = mockMvc.perform(post("/api/v1/groups/standalone")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Private\"}"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        long groupId = Long.parseLong(readJsonNumberField(createResponse, "id"));

        mockMvc.perform(get("/api/v1/groups/" + groupId).header("Authorization", "Bearer " + outsiderToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTHORIZATION_ERROR"));
    }
}
