package com.jodngan.course;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import com.jodngan.support.FixedUserTestConfiguration;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@Import(FixedUserTestConfiguration.class)
@Testcontainers(disabledWithoutDocker = true)
class CourseApiIntegrationTests {

    @Container
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18-alpine")
            .withDatabaseName("jodngan")
            .withUsername("jodngan")
            .withPassword("test-password");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void clearData() {
        jdbcTemplate.update("DELETE FROM tasks");
        jdbcTemplate.update("DELETE FROM courses");
        jdbcTemplate.update("DELETE FROM users");
        Instant now = Instant.parse("2026-10-29T00:00:00Z");
        jdbcTemplate.update(
                "INSERT INTO users (id, email, password_hash, time_zone, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?)",
                FixedUserTestConfiguration.USER_ID,
                "test@example.com",
                "test-hash",
                "Asia/Bangkok",
                java.sql.Timestamp.from(now),
                java.sql.Timestamp.from(now));
    }

    @DynamicPropertySource
    static void configureDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Test
    void createsListsRenamesAndRemovesCourses() throws Exception {
        createCourse("  Software Engineering  ").andExpect(status().isCreated());
        MvcResult dataWarehouse = createCourse("Data Warehouse")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Data Warehouse"))
                .andReturn();
        String courseId = objectMapper.readTree(dataWarehouse.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(get("/api/courses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[0].name").value("Data Warehouse"))
                .andExpect(jsonPath("$.items[1].name").value("Software Engineering"));

        mockMvc.perform(put("/api/courses/{courseId}", courseId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", "Database Systems"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Database Systems"));

        mockMvc.perform(delete("/api/courses/{courseId}", courseId))
                .andExpect(status().isNoContent());
        assertEquals(1L, jdbcTemplate.queryForObject("SELECT count(*) FROM courses", Long.class));
    }

    @Test
    void taskCanBeAssignedFilteredAndSurvivesCourseRemoval() throws Exception {
        MvcResult courseResult = createCourse("ISD").andExpect(status().isCreated()).andReturn();
        JsonNode course = objectMapper.readTree(courseResult.getResponse().getContentAsString());
        String courseId = course.get("id").asText();
        Instant dueAt = Instant.parse("2030-03-15T13:45:00Z");

        MvcResult assignedResult = mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", "Course assignment",
                                "dueAt", dueAt.toString(),
                                "courseId", courseId))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.course.id").value(courseId))
                .andExpect(jsonPath("$.course.name").value("ISD"))
                .andReturn();
        String taskId = objectMapper.readTree(assignedResult.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", "No course",
                                "dueAt", dueAt.plusSeconds(3600).toString()))))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/tasks").param("courseId", courseId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].id").value(taskId));

        mockMvc.perform(delete("/api/courses/{courseId}", courseId))
                .andExpect(status().isNoContent());

        MvcResult taskAfterDelete = mockMvc.perform(get("/api/tasks/{taskId}", taskId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Course assignment"))
                .andReturn();
        JsonNode task = objectMapper.readTree(taskAfterDelete.getResponse().getContentAsString());
        assertTrue(task.get("course") == null || task.get("course").isNull());
        assertEquals(2L, jdbcTemplate.queryForObject("SELECT count(*) FROM tasks", Long.class));
    }

    @Test
    void taskCourseCanBeChangedAndClearedFromDetailUpdate() throws Exception {
        String firstCourseId = objectMapper.readTree(
                createCourse("ISD").andExpect(status().isCreated()).andReturn().getResponse().getContentAsString())
                .get("id").asText();
        String secondCourseId = objectMapper.readTree(
                createCourse("Cloud").andExpect(status().isCreated()).andReturn().getResponse().getContentAsString())
                .get("id").asText();
        String dueAt = "2030-03-15T13:45:00Z";

        MvcResult created = mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", "Move course",
                                "dueAt", dueAt,
                                "courseId", firstCourseId))))
                .andExpect(status().isCreated())
                .andReturn();
        String taskId = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(put("/api/tasks/{taskId}", taskId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", "Move course",
                                "dueAt", dueAt,
                                "courseId", secondCourseId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.course.id").value(secondCourseId))
                .andExpect(jsonPath("$.course.name").value("Cloud"));

        MvcResult cleared = mockMvc.perform(put("/api/tasks/{taskId}", taskId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", "Move course",
                                "dueAt", dueAt))))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode clearedTask = objectMapper.readTree(cleared.getResponse().getContentAsString());
        assertTrue(clearedTask.get("course") == null || clearedTask.get("course").isNull());
    }

    @Test
    void assigningUnknownCourseReturnsNotFound() throws Exception {
        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", "Unknown course",
                                "dueAt", "2030-03-15T13:45:00Z",
                                "courseId", UUID.randomUUID().toString()))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Course was not found"));
    }

    @Test
    void rejectsBlankAndOverlongCourseNames() throws Exception {
        createCourse("   ")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.name").exists());
        createCourse("x".repeat(121))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.name").exists());
    }

    private org.springframework.test.web.servlet.ResultActions createCourse(String name) throws Exception {
        return mockMvc.perform(post("/api/courses")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("name", name))));
    }
}
