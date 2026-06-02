package cn.suhoan.asihavewritten.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import cn.suhoan.asihavewritten.log.LogIngestService;
import cn.suhoan.asihavewritten.log.LogSource;
import cn.suhoan.asihavewritten.log.LogSourceService;
import cn.suhoan.asihavewritten.TestMongoProperties;

import static cn.suhoan.asihavewritten.TestMongoProperties.ENV_NAME;

@SpringBootTest
@AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named = ENV_NAME, matches = ".+")
class IngestControllerTests {

    @DynamicPropertySource
    static void mongoProperties(DynamicPropertyRegistry registry) {
        TestMongoProperties.register(registry);
    }

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    LogSourceService sourceService;

    @MockitoBean
    LogIngestService ingestService;

    @Test
    void acceptsSingleLogWithValidApiKey() throws Exception {
        when(sourceService.authenticate("secret")).thenReturn(Optional.of(new LogSource(
                "default",
                "billing",
                "node-1",
                "hash",
                "encrypted",
                true,
                Instant.now())));
        when(ingestService.accept(any())).thenReturn(true);

        mockMvc.perform(post("/api/logs")
                        .header("X-API-Key", "secret")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "service": "billing",
                                  "environment": "prod",
                                  "level": "ERROR",
                                  "message": "payment failed"
                                }
                                """))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.accepted").value(1));
    }

    @Test
    void rejectsInvalidApiKey() throws Exception {
        when(sourceService.authenticate("bad")).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/logs")
                        .header("X-API-Key", "bad")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "service": "billing",
                                  "environment": "prod",
                                  "level": "ERROR",
                                  "message": "payment failed"
                                }
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void acceptsNdjsonStream() throws Exception {
        when(sourceService.authenticate("secret")).thenReturn(Optional.of(new LogSource(
                "default",
                "billing",
                "node-1",
                "hash",
                "encrypted",
                true,
                Instant.now())));
        when(ingestService.accept(any())).thenReturn(true);

        mockMvc.perform(post("/api/logs/stream")
                        .header("X-API-Key", "secret")
                        .contentType("application/x-ndjson")
                        .content("""
                                {"service":"billing","environment":"prod","level":"INFO","message":"one"}
                                {"service":"billing","environment":"prod","level":"WARN","message":"two"}
                                """))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.accepted").value(2));
    }

    @Test
    void redirectsUnauthenticatedWebUiButNotIngestApi() throws Exception {
        when(sourceService.authenticate("secret")).thenReturn(Optional.of(new LogSource(
                "default",
                "billing",
                "node-1",
                "hash",
                "encrypted",
                true,
                Instant.now())));
        when(ingestService.accept(any())).thenReturn(true);

        mockMvc.perform(get("/ui/logs?service=billing"))
                .andExpect(status().is3xxRedirection())
                .andExpect(result -> assertThat(result.getResponse().getRedirectedUrl()).contains("/ui/login?redirect="));

        mockMvc.perform(post("/api/logs")
                        .header("X-API-Key", "secret")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "service": "spoofed",
                                  "environment": "prod",
                                  "level": "INFO",
                                  "message": "api remains public"
                                }
                                """))
                .andExpect(status().isAccepted());
    }
}
