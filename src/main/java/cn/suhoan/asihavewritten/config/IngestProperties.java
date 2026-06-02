package cn.suhoan.asihavewritten.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "aih.ingest")
public record IngestProperties(
        int queueCapacity,
        int batchSize,
        Duration flushInterval,
        String defaultServiceName,
        String defaultServiceDisplayName,
        String defaultInstanceName,
        String defaultApiKey) {
}
