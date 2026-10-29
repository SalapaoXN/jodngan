package com.jodngan.identity;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class AuthOwnershipIntegrationTests {

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

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void clearData() {
        jdbcTemplate.update("DELETE FROM tasks");
        jdbcTemplate.update("DELETE FROM courses");
        jdbcTemplate.update("DELETE FROM users");
    }

    @DynamicPropertySource
    static void configureDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Test
    void exposesCsrfAndProtectsAuthenticatedApis() throws Exception {
        mockMvc.perform(get("/api/csrf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.headerName").isNotEmpty())
                .andExpect(jsonPath("$.parameterName").isNotEmpty())
                .andExpect(jsonPath("$.token").isNotEmpty());

        mockMvc.perform(get("/api/tasks"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "student@example.com",
                                "password", "password123",
                                "timeZone", "Asia/Bangkok"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void registrationNormalizesEmailHashesPasswordAndCreatesReusableSession() throws Exception {
        SessionUser user = register("Student@Example.COM", "password123");

        String storedHash = jdbcTemplate.queryForObject(
                "SELECT password_hash FROM users WHERE id = ?",
                String.class,
                java.util.UUID.fromString(user.userId()));
        assertNotNull(storedHash);
        assertNotEquals("password123", storedHash);
        assertTrue(passwordEncoder.matches("password123", storedHash));

        mockMvc.perform(get("/api/auth/me").session(user.session()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.id").value(user.userId()))
                .andExpect(jsonPath("$.user.email").value("student@example.com"))
                .andExpect(jsonPath("$.user.timeZone").value("Asia/Bangkok"));
    }

    @Test
    void duplicateEmailAndInvalidLoginUseSafeErrors() throws Exception {
        register("student@example.com", "password123");

        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "STUDENT@example.com",
                                "password", "another-password",
                                "timeZone", "Asia/Bangkok"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_EXISTS"));

        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "student@example.com",
                                "password", "wrong-password"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.message").value("Email or password is incorrect"));

        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "missing@example.com",
                                "password", "wrong-password"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.message").value("Email or password is incorrect"));
    }

    @Test
    void logoutInvalidatesTheAuthenticatedSession() throws Exception {
        SessionUser user = register("student@example.com", "password123");

        mockMvc.perform(post("/api/auth/logout").session(user.session()).with(csrf()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/auth/me").session(user.session()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void usersCannotAccessOrAssignEachOthersResources() throws Exception {
        SessionUser alice = register("alice@example.com", "password123");
        String aliceCourseId = createCourse(alice.session(), "ISD");
        String aliceTaskId = createTask(alice.session(), "Alice task", aliceCourseId);

        SessionUser bob = register("bob@example.com", "password123");

        mockMvc.perform(get("/api/tasks/{taskId}", aliceTaskId).session(bob.session()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));

        mockMvc.perform(put("/api/tasks/{taskId}", aliceTaskId)
                        .session(bob.session())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", "Stolen task",
                                "dueAt", "2030-03-15T13:45:00Z"))))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/tasks/{taskId}/complete", aliceTaskId)
                        .session(bob.session())
                        .with(csrf()))
                .andExpect(status().isNotFound());

        mockMvc.perform(put("/api/courses/{courseId}", aliceCourseId)
                        .session(bob.session())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", "Stolen course"))))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/api/courses/{courseId}", aliceCourseId)
                        .session(bob.session())
                        .with(csrf()))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/tasks")
                        .session(bob.session())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", "Cross-user course",
                                "dueAt", "2030-03-15T13:45:00Z",
                                "courseId", aliceCourseId))))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/tasks")
                        .session(bob.session())
                        .param("courseId", aliceCourseId))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/tasks").session(bob.session()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(0));

        mockMvc.perform(get("/api/courses").session(bob.session()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(0));

        mockMvc.perform(get("/api/today").session(bob.session()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.primaryRecommendation").doesNotExist());

        mockMvc.perform(get("/api/tasks/{taskId}", aliceTaskId).session(alice.session()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Alice task"));
    }

    private SessionUser register(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", email,
                                "password", password,
                                "timeZone", "Asia/Bangkok"))))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
        assertNotNull(session);
        return new SessionUser(session, body.get("user").get("id").asText());
    }

    private String createCourse(MockHttpSession session, String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/courses")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", name))))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    private String createTask(MockHttpSession session, String title, String courseId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/tasks")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", title,
                                "dueAt", "2030-03-15T13:45:00Z",
                                "courseId", courseId))))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    private record SessionUser(MockHttpSession session, String userId) {
    }
}
