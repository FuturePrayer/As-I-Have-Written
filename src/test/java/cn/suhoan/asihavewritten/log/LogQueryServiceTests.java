package cn.suhoan.asihavewritten.log;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import cn.suhoan.asihavewritten.TestMongoProperties;

import static cn.suhoan.asihavewritten.TestMongoProperties.ENV_NAME;

@SpringBootTest
@EnabledIfEnvironmentVariable(named = ENV_NAME, matches = ".+")
class LogQueryServiceTests {

    @DynamicPropertySource
    static void mongoProperties(DynamicPropertyRegistry registry) {
        TestMongoProperties.register(registry);
    }

    @Autowired
    LogEntryRepository repository;

    @Autowired
    LogQueryService queryService;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    void searchesByApplicationGeneratedTokens() {
        repository.save(new LogEntry(
                Instant.parse("2026-06-02T01:00:00Z"),
                Instant.parse("2026-06-02T01:00:01Z"),
                "source-1",
                "billing",
                "node-1",
                "prod",
                LogLevel.ERROR,
                "trace-1",
                null,
                "payment failed",
                Map.of("threadName", "worker-1"),
                List.of("payment", "failed", "支付", "失败"),
                IngestChannel.HTTP));

        Page<LogEntry> result = queryService.search(new LogQuery(
                null,
                null,
                "billing",
                null,
                "prod",
                LogLevel.ERROR,
                null,
                null,
                Map.of("threadName", "worker-1"),
                List.of("payment", "failed"),
                0,
                20));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().getMessage()).isEqualTo("payment failed");
        assertThat(queryService.discoverMetadataKeys("billing", null, null)).contains("threadName");
    }
}
