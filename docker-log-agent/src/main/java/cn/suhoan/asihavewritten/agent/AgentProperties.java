package cn.suhoan.asihavewritten.agent;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Validated
@ConfigurationProperties(prefix = "aih.agent")
public record AgentProperties(
        @NotBlank String endpoint,
        String apiKeyFile,
        String apiKeys,
        @NotBlank String defaultEnvironment,
        String logRegex,
        String logRegexMetadataGroups,
        @Min(1) int batchSize,
        @Min(1) int queueCapacity,
        @NotNull Duration flushInterval,
        @NotNull Duration requestTimeout,
        String dockerHost) {
}
