package com.jodngan.task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TaskApiIntegrationTests.FixedClockConfiguration.class)
@Testcontainers(disabledWithoutDocker = true)
class TaskApiIntegrationTests {

    private static final Instant FIXED_NOW = Instant.parse("2026-10-29T00:00:00Z");

    @Container
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18-alpine")
            .withDatabaseName("jodngan")
            .withUsername("jodngan")
            .withPassword("test-password");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private Flyway flyway;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void clearTasks() {
        jdbcTemplate.update("DELETE FROM tasks");
    }

    @DynamicPropertySource
    static void configureDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Test
    void flywayCreatesTaskSchemaThroughPlanningEnrichmentMigration() {
        assertTrue(Arrays.stream(flyway.info().applied())
                .anyMatch(migration -> migration.getVersion() != null
                        && "1".equals(migration.getVersion().getVersion())));
        assertTrue(Arrays.stream(flyway.info().applied())
                .anyMatch(migration -> migration.getVersion() != null
                        && "2".equals(migration.getVersion().getVersion())));
        assertEquals(0L, jdbcTemplate.queryForObject(
                "SELECT count(*) FROM information_schema.tables "
                        + "WHERE table_schema = 'public' AND table_name = 'users'",
                Long.class));
        assertEquals(3L, jdbcTemplate.queryForObject(
                "SELECT count(*) FROM information_schema.columns "
                        + "WHERE table_schema = 'public' AND table_name = 'tasks' "
                        + "AND column_name IN ('estimated_minutes', 'importance', 'notes')",
                Long.class));
        assertEquals(0L, jdbcTemplate.queryForObject(
                "SELECT count(*) FROM information_schema.columns "
                        + "WHERE table_schema = 'public' AND table_name = 'tasks' "
                        + "AND column_name IN ('user_id', 'course_id')",
                Long.class));
    }

    @Test
    void createsTaskTrimsTitlePersistsItAndReturnsFutureDeadlineProgress() throws Exception {
        Instant dueAt = FIXED_NOW.plusSeconds(86_400);
        MvcResult result = postTask("  Database Assignment  ", dueAt)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Database Assignment"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.dueAt").value(dueAt.toString()))
                .andExpect(jsonPath("$.estimatedMinutes").doesNotExist())
                .andExpect(jsonPath("$.importance").doesNotExist())
                .andExpect(jsonPath("$.notes").doesNotExist())
                .andExpect(jsonPath("$.planning.remainingSeconds").value(86_400))
                .andExpect(jsonPath("$.planning.overdue").value(false))
                .andExpect(jsonPath("$.planning.risk.state").value("INSUFFICIENT_DATA"))
                .andExpect(jsonPath("$.planning.risk.reasonCode").value("EFFORT_UNKNOWN"))
                .andReturn();
        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());

        String persistedTitle = jdbcTemplate.queryForObject(
                "SELECT title FROM tasks WHERE id = ?", String.class, UUID.fromString(response.get("id").asText()));
        assertEquals("Database Assignment", persistedTitle);
    }

    @Test
    void rejectsBlankTitleWithValidationEnvelope() throws Exception {
        postTask("   ", FIXED_NOW.plusSeconds(60))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors.title").exists());
        assertEquals(0L, taskCount());
    }

    @Test
    void rejectsTitlesLongerThanTwoHundredCharacters() throws Exception {
        postTask("x".repeat(201), FIXED_NOW.plusSeconds(60))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors.title").exists());
        assertEquals(0L, taskCount());
    }

    @Test
    void rejectsMissingDueAtWithValidationEnvelope() throws Exception {
        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("title", "No due date"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors.dueAt").exists());
        assertEquals(0L, taskCount());
    }

    @Test
    void acceptsPastDeadlineAndMarksItOverdue() throws Exception {
        Instant dueAt = FIXED_NOW.minusSeconds(3 * 3_600L);
        postTask("Past deadline", dueAt)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.planning.remainingSeconds").value(-10_800))
                .andExpect(jsonPath("$.planning.overdue").value(true));
        assertEquals(1L, taskCount());
    }

    @Test
    void deadlineExactlyNowIsNotYetOverdue() throws Exception {
        postTask("Due now", FIXED_NOW)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.planning.remainingSeconds").value(0))
                .andExpect(jsonPath("$.planning.overdue").value(false));
    }

    @Test
    void listsActiveTasksByDeadlineThenIdAndReturnsPlanningData() throws Exception {
        postTask("Later", FIXED_NOW.plusSeconds(3_600)).andExpect(status().isCreated());
        postTask("Early B", FIXED_NOW.plusSeconds(1_800)).andExpect(status().isCreated());
        postTask("Early A", FIXED_NOW.plusSeconds(1_800)).andExpect(status().isCreated());

        MvcResult result = mockMvc.perform(get("/api/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(3))
                .andExpect(jsonPath("$.items[0].planning.remainingSeconds").exists())
                .andReturn();

        JsonNode items = objectMapper.readTree(result.getResponse().getContentAsString()).get("items");
        List<JsonNode> actual = new ArrayList<>();
        items.forEach(actual::add);
        List<JsonNode> expected = actual.stream()
                .sorted(Comparator.comparing((JsonNode item) -> Instant.parse(item.get("dueAt").asText()))
                        .thenComparing(item -> item.get("id").asText()))
                .toList();
        assertEquals(expected.stream().map(item -> item.get("id").asText()).toList(),
                actual.stream().map(item -> item.get("id").asText()).toList());
        assertTrue(actual.get(0).get("dueAt").asText().compareTo(actual.get(2).get("dueAt").asText()) < 0);
        assertEquals("Later", actual.get(2).get("title").asText());
    }

    @Test
    void getsTaskByIdAndReturnsNotFoundEnvelopeForUnknownId() throws Exception {
        MvcResult result = postTask("Fetch this task", FIXED_NOW.plusSeconds(600))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode created = objectMapper.readTree(result.getResponse().getContentAsString());
        String id = created.get("id").asText();

        mockMvc.perform(get("/api/tasks/{taskId}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.title").value("Fetch this task"));

        mockMvc.perform(get("/api/tasks/{taskId}", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void todayReturnsEmptyRecommendationWhenThereAreNoActiveTasks() throws Exception {
        mockMvc.perform(get("/api/today"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.generatedAt").value(FIXED_NOW.toString()))
                .andExpect(jsonPath("$.primaryRecommendation").doesNotExist())
                .andExpect(jsonPath("$.upcomingTasks.length()").value(0));
    }

    @Test
    void todayRecommendsTheMostUrgentTaskWithReasonAndStartGuidance() throws Exception {
        postTask("Later task", FIXED_NOW.plusSeconds(4 * 86_400L)).andExpect(status().isCreated());
        postTask("Due soon", FIXED_NOW.plusSeconds(12 * 3_600L)).andExpect(status().isCreated());
        postTask("Overdue task", FIXED_NOW.minusSeconds(60)).andExpect(status().isCreated());

        mockMvc.perform(get("/api/today"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.generatedAt").value(FIXED_NOW.toString()))
                .andExpect(jsonPath("$.primaryRecommendation.task.title").value("Overdue task"))
                .andExpect(jsonPath("$.primaryRecommendation.task.planning.overdue").value(true))
                .andExpect(jsonPath("$.primaryRecommendation.urgency").value("OVERDUE"))
                .andExpect(jsonPath("$.primaryRecommendation.reasonCode").value("OVERDUE"))
                .andExpect(jsonPath("$.primaryRecommendation.reason").value("เลยกำหนดส่งแล้ว"))
                .andExpect(jsonPath("$.primaryRecommendation.startGuidance").value("START_NOW"))
                .andExpect(jsonPath("$.upcomingTasks[0].task.title").value("Due soon"))
                .andExpect(jsonPath("$.upcomingTasks[1].task.title").value("Later task"));
    }

    @Test
    void todayExcludesCompletedTasksAndCapsUpcomingAtFive() throws Exception {
        MvcResult completedResult = postTask("Completed urgent", FIXED_NOW.minusSeconds(60))
                .andExpect(status().isCreated())
                .andReturn();
        UUID completedId = UUID.fromString(
                objectMapper.readTree(completedResult.getResponse().getContentAsString()).get("id").asText());
        jdbcTemplate.update(
                "UPDATE tasks SET status = 'COMPLETED', completed_at = ? WHERE id = ?",
                java.sql.Timestamp.from(FIXED_NOW),
                completedId);

        for (int day = 1; day <= 7; day++) {
            postTask("Active " + day, FIXED_NOW.plusSeconds(day * 86_400L))
                    .andExpect(status().isCreated());
        }

        mockMvc.perform(get("/api/today"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.primaryRecommendation.task.title").value("Active 1"))
                .andExpect(jsonPath("$.upcomingTasks.length()").value(5))
                .andExpect(jsonPath("$.upcomingTasks[0].task.title").value("Active 2"))
                .andExpect(jsonPath("$.upcomingTasks[4].task.title").value("Active 6"));
    }

    @Test
    void completeMovesTaskOutOfActiveAndTodayThenReopenReturnsIt() throws Exception {
        MvcResult createdResult = postTask("Lifecycle task", FIXED_NOW.plusSeconds(3_600))
                .andExpect(status().isCreated())
                .andReturn();
        String id = objectMapper.readTree(createdResult.getResponse().getContentAsString()).get("id").asText();

        MvcResult completedResult = mockMvc.perform(post("/api/tasks/{taskId}/complete", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.completedAt").value(FIXED_NOW.toString()))
                .andExpect(jsonPath("$.planning.overdue").value(false))
                .andReturn();
        JsonNode completed = objectMapper.readTree(completedResult.getResponse().getContentAsString());
        assertEquals(FIXED_NOW.toString(), completed.get("completedAt").asText());

        mockMvc.perform(get("/api/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(0));
        mockMvc.perform(get("/api/tasks").param("status", "COMPLETED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].id").value(id));
        mockMvc.perform(get("/api/today"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.primaryRecommendation").doesNotExist());

        mockMvc.perform(post("/api/tasks/{taskId}/complete", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completedAt").value(FIXED_NOW.toString()));

        MvcResult reopenedResult = mockMvc.perform(post("/api/tasks/{taskId}/reopen", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andReturn();
        JsonNode reopened = objectMapper.readTree(reopenedResult.getResponse().getContentAsString());
        assertTrue(reopened.get("completedAt").isNull());

        mockMvc.perform(get("/api/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(id));
        mockMvc.perform(get("/api/today"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.primaryRecommendation.task.id").value(id));
    }

    @Test
    void updatesTaskTitleAndDeadlineWithoutChangingLifecycleState() throws Exception {
        MvcResult createdResult = postTask("Original title", FIXED_NOW.plusSeconds(3_600))
                .andExpect(status().isCreated())
                .andReturn();
        String id = objectMapper.readTree(createdResult.getResponse().getContentAsString()).get("id").asText();
        Instant newDueAt = FIXED_NOW.plusSeconds(7_200);

        mockMvc.perform(put("/api/tasks/{taskId}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", "  Updated title  ",
                                "dueAt", newDueAt.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated title"))
                .andExpect(jsonPath("$.dueAt").value(newDueAt.toString()))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.planning.remainingSeconds").value(7_200));

        mockMvc.perform(get("/api/tasks/{taskId}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated title"))
                .andExpect(jsonPath("$.dueAt").value(newDueAt.toString()));
    }

    @Test
    void updatesAndClearsOptionalPlanningFields() throws Exception {
        MvcResult createdResult = postTask("Planning task", FIXED_NOW.plusSeconds(7 * 86_400L))
                .andExpect(status().isCreated())
                .andReturn();
        String id = objectMapper.readTree(createdResult.getResponse().getContentAsString()).get("id").asText();
        Instant dueAt = FIXED_NOW.plusSeconds(7 * 86_400L);

        mockMvc.perform(put("/api/tasks/{taskId}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", "Planning task",
                                "dueAt", dueAt.toString(),
                                "estimatedMinutes", 180,
                                "importance", "HIGH",
                                "notes", "  Split the report into sections  "))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estimatedMinutes").value(180))
                .andExpect(jsonPath("$.importance").value("HIGH"))
                .andExpect(jsonPath("$.notes").value("Split the report into sections"))
                .andExpect(jsonPath("$.planning.risk.state").value("NO_CONFIRMED_RISK"));

        assertEquals(180, jdbcTemplate.queryForObject(
                "SELECT estimated_minutes FROM tasks WHERE id = ?", Integer.class, UUID.fromString(id)));
        assertEquals("HIGH", jdbcTemplate.queryForObject(
                "SELECT importance FROM tasks WHERE id = ?", String.class, UUID.fromString(id)));

        mockMvc.perform(put("/api/tasks/{taskId}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", "Planning task",
                                "dueAt", dueAt.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estimatedMinutes").doesNotExist())
                .andExpect(jsonPath("$.importance").doesNotExist())
                .andExpect(jsonPath("$.notes").doesNotExist())
                .andExpect(jsonPath("$.planning.risk.state").value("INSUFFICIENT_DATA"))
                .andExpect(jsonPath("$.planning.risk.reasonCode").value("EFFORT_UNKNOWN"));

        assertEquals(0L, jdbcTemplate.queryForObject(
                "SELECT count(*) FROM tasks WHERE id = ? AND estimated_minutes IS NOT NULL",
                Long.class,
                UUID.fromString(id)));
    }

    @Test
    void rejectsNonPositiveEstimatedEffort() throws Exception {
        MvcResult createdResult = postTask("Invalid effort task", FIXED_NOW.plusSeconds(86_400))
                .andExpect(status().isCreated())
                .andReturn();
        String id = objectMapper.readTree(createdResult.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(put("/api/tasks/{taskId}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", "Invalid effort task",
                                "dueAt", FIXED_NOW.plusSeconds(86_400).toString(),
                                "estimatedMinutes", 0))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors.estimatedMinutes").exists());
    }

    @Test
    void enrichingEffortCanImmediatelyChangeTodayRecommendation() throws Exception {
        Instant earlierDueAt = FIXED_NOW.plusSeconds(4 * 86_400L);
        Instant laterDueAt = FIXED_NOW.plusSeconds(4 * 86_400L + 3_600);
        postTask("Earlier small task", earlierDueAt).andExpect(status().isCreated());
        MvcResult laterResult = postTask("Later large task", laterDueAt)
                .andExpect(status().isCreated())
                .andReturn();
        String laterId = objectMapper.readTree(laterResult.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(get("/api/today"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.primaryRecommendation.task.title").value("Earlier small task"));

        mockMvc.perform(put("/api/tasks/{taskId}", laterId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", "Later large task",
                                "dueAt", laterDueAt.toString(),
                                "estimatedMinutes", 1_800))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/today"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.primaryRecommendation.task.id").value(laterId))
                .andExpect(jsonPath("$.primaryRecommendation.reasonCode").value("HIGH_EFFORT_PRESSURE"))
                .andExpect(jsonPath("$.primaryRecommendation.startGuidance").value("START_TODAY"))
                .andExpect(jsonPath("$.primaryRecommendation.task.planning.risk.state").value("NO_CONFIRMED_RISK"));
    }

    @Test
    void effortEqualToClockTimeIsReportedAtRisk() throws Exception {
        Instant dueAt = FIXED_NOW.plusSeconds(120 * 60L);
        MvcResult createdResult = postTask("Tight task", dueAt)
                .andExpect(status().isCreated())
                .andReturn();
        String id = objectMapper.readTree(createdResult.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(put("/api/tasks/{taskId}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", "Tight task",
                                "dueAt", dueAt.toString(),
                                "estimatedMinutes", 120))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.planning.risk.state").value("AT_RISK"))
                .andExpect(jsonPath("$.planning.risk.reasonCode").value("INSUFFICIENT_CLOCK_TIME"));
    }

    @Test
    void rejectsUnknownTaskStatusAsValidationError() throws Exception {
        mockMvc.perform(get("/api/tasks").param("status", "ARCHIVED"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("Request parameter is invalid"));
    }

    @Test
    void listsCompletedTasksByMostRecentCompletionFirst() throws Exception {
        MvcResult olderResult = postTask("Older completion", FIXED_NOW.plusSeconds(3_600))
                .andExpect(status().isCreated())
                .andReturn();
        MvcResult newerResult = postTask("Newer completion", FIXED_NOW.plusSeconds(7_200))
                .andExpect(status().isCreated())
                .andReturn();
        UUID olderId = UUID.fromString(
                objectMapper.readTree(olderResult.getResponse().getContentAsString()).get("id").asText());
        UUID newerId = UUID.fromString(
                objectMapper.readTree(newerResult.getResponse().getContentAsString()).get("id").asText());

        jdbcTemplate.update(
                "UPDATE tasks SET status = 'COMPLETED', completed_at = ?, updated_at = ? WHERE id = ?",
                java.sql.Timestamp.from(FIXED_NOW.plusSeconds(60)),
                java.sql.Timestamp.from(FIXED_NOW.plusSeconds(60)),
                olderId);
        jdbcTemplate.update(
                "UPDATE tasks SET status = 'COMPLETED', completed_at = ?, updated_at = ? WHERE id = ?",
                java.sql.Timestamp.from(FIXED_NOW.plusSeconds(120)),
                java.sql.Timestamp.from(FIXED_NOW.plusSeconds(120)),
                newerId);

        mockMvc.perform(get("/api/tasks").param("status", "COMPLETED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].title").value("Newer completion"))
                .andExpect(jsonPath("$.items[1].title").value("Older completion"));
    }

    private org.springframework.test.web.servlet.ResultActions postTask(String title, Instant dueAt)
            throws Exception {
        return mockMvc.perform(post("/api/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("title", title, "dueAt", dueAt.toString()))));
    }

    private long taskCount() {
        return jdbcTemplate.queryForObject("SELECT count(*) FROM tasks", Long.class);
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class FixedClockConfiguration {

        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(FIXED_NOW, ZoneOffset.UTC);
        }
    }
}
