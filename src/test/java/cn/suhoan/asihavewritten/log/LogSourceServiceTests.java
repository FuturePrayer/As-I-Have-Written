package cn.suhoan.asihavewritten.log;

import static cn.suhoan.asihavewritten.TestMongoProperties.ENV_NAME;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import cn.suhoan.asihavewritten.TestMongoProperties;

@SpringBootTest
@EnabledIfEnvironmentVariable(named = ENV_NAME, matches = ".+")
class LogSourceServiceTests {

    @DynamicPropertySource
    static void mongoProperties(DynamicPropertyRegistry registry) {
        TestMongoProperties.register(registry);
    }

    @Autowired
    LogSourceService sourceService;

    @Autowired
    LogSourceRepository sourceRepository;

    @Autowired
    LogServiceRepository serviceRepository;

    @Autowired
    LogEntryRepository entryRepository;

    @BeforeEach
    void setUp() {
        sourceRepository.deleteAll();
        serviceRepository.deleteAll();
        entryRepository.deleteAll();
    }

    @Test
    void deletingApiKeyKeepsServiceUntilNoLogsRemain() {
        LogSourceService.CreatedLogSource created = sourceService.createSource("billing", "Billing", "node-1");
        String sourceId = created.source().getId();

        entryRepository.save(new LogEntry(
                Instant.parse("2026-06-02T01:00:00Z"),
                Instant.parse("2026-06-02T01:00:01Z"),
                sourceId,
                "billing",
                "node-1",
                "prod",
                LogLevel.INFO,
                "trace-1",
                null,
                "created",
                Map.of(),
                List.of("created"),
                IngestChannel.HTTP));

        sourceService.deleteSource(sourceId);

        assertThat(sourceRepository.count()).isZero();
        assertThat(serviceRepository.findByName("billing")).isPresent();

        sourceService.cleanupUnusedServices();
        assertThat(serviceRepository.findByName("billing")).isPresent();

        entryRepository.deleteAll();
        sourceService.cleanupUnusedServices();
        assertThat(serviceRepository.findByName("billing")).isEmpty();
    }

    @Test
    void sourceViewsExposeDisplayNameAndMaskedApiKey() {
        sourceService.createSource("payments", "Payments API", "node-1");

        LogSourceService.LogSourceView view = sourceService.sources().getFirst();

        assertThat(view.serviceDisplayName()).isEqualTo("Payments API");
        assertThat(view.maskedApiKey()).startsWith(view.apiKey().substring(0, 4));
        assertThat(view.maskedApiKey()).endsWith(view.apiKey().substring(view.apiKey().length() - 3));
        assertThat(view.maskedApiKey()).doesNotContain(view.apiKey());
    }
}
