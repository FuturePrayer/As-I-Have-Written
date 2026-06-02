package cn.suhoan.asihavewritten.agent;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class ContainerLogParserTests {

    private final ContainerLogParser parser = new ContainerLogParser();

    @Test
    void fallsBackToPlainTextMapping() {
        ContainerLogConfig config = config("", List.of());
        Instant now = Instant.parse("2026-06-02T12:00:00Z");

        LogPayload payload = parser.parse(config, DockerStream.STDERR, now, "plain failure");

        assertThat(payload.eventTime()).isEqualTo(now);
        assertThat(payload.level()).isEqualTo(LogLevel.ERROR);
        assertThat(payload.message()).isEqualTo("plain failure");
        assertThat(payload.metadata()).containsEntry("containerName", "payment-1");
        assertThat(payload.metadata()).containsEntry("stream", "stderr");
    }

    @Test
    void parsesNamedRegexGroupsAndMetadataGroups() {
        ContainerLogConfig config = config(
                "^(?<time>\\S+) (?<level>\\w+) trace=(?<traceId>\\S+) span=(?<spanId>\\S+) tenant=(?<tenant>\\S+) (?<message>.*)$",
                List.of("tenant"));
        Instant collectedAt = Instant.parse("2026-06-02T12:00:00Z");

        LogPayload payload = parser.parse(config, DockerStream.STDOUT, collectedAt,
                "2026-06-02T11:59:58Z warn trace=t-1 span=s-1 tenant=acme payment failed");

        assertThat(payload.eventTime()).isEqualTo(Instant.parse("2026-06-02T11:59:58Z"));
        assertThat(payload.level()).isEqualTo(LogLevel.WARN);
        assertThat(payload.traceId()).isEqualTo("t-1");
        assertThat(payload.spanId()).isEqualTo("s-1");
        assertThat(payload.message()).isEqualTo("payment failed");
        assertThat(payload.metadata()).containsEntry("tenant", "acme");
    }

    @Test
    void invalidLevelAndTimeUseFallbacks() {
        ContainerLogConfig config = config("^(?<time>\\S+) (?<level>\\w+) (?<message>.*)$", List.of());
        Instant collectedAt = Instant.parse("2026-06-02T12:00:00Z");

        LogPayload payload = parser.parse(config, DockerStream.STDOUT, collectedAt, "bad-date noisy hello");

        assertThat(payload.eventTime()).isEqualTo(collectedAt);
        assertThat(payload.level()).isEqualTo(LogLevel.INFO);
        assertThat(payload.message()).isEqualTo("hello");
    }

    private ContainerLogConfig config(String regex, List<String> metadataGroups) {
        return new ContainerLogConfig(
                "abcdef123456",
                "payment-1",
                "example/payment:latest",
                "payment-key",
                "payment",
                "payment-1",
                "prod",
                regex,
                metadataGroups,
                "shop",
                "payment",
                Map.of("safe.label", "visible"));
    }
}
