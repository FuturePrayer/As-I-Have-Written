package cn.suhoan.asihavewritten.agent;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ApiKeyRegistryTests {

    @TempDir
    Path tempDir;

    @Test
    void parsesInlineApiKeys() {
        Map<String, String> keys = ApiKeyRegistry.parseInline("app=key-1; worker = key-2 ; invalid");

        assertThat(keys).containsEntry("app", "key-1");
        assertThat(keys).containsEntry("worker", "key-2");
        assertThat(keys).doesNotContainKey("invalid");
    }

    @Test
    void fileKeysOverrideInlineKeys() throws IOException {
        Path keyFile = tempDir.resolve("keys.properties");
        Files.writeString(keyFile, "app=file-key\nworker=worker-key\n");
        AgentProperties properties = new AgentProperties(
                "http://localhost:25091/api/logs/batch",
                keyFile.toString(),
                "app=inline-key",
                "local",
                "",
                "",
                100,
                1000,
                Duration.ofSeconds(1),
                Duration.ofSeconds(5),
                "");

        ApiKeyRegistry registry = new ApiKeyRegistry(properties);

        assertThat(registry.find("app")).contains("file-key");
        assertThat(registry.find("worker")).contains("worker-key");
        assertThat(registry.find("missing")).isEmpty();
    }
}
