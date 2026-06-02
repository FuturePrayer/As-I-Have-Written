package cn.suhoan.asihavewritten.agent;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ApiKeyRegistry {

    private static final Logger log = LoggerFactory.getLogger(ApiKeyRegistry.class);

    private final Map<String, String> apiKeys;

    public ApiKeyRegistry(AgentProperties properties) {
        this.apiKeys = load(properties);
        log.info("Loaded {} As I Have Written API key reference(s)", apiKeys.size());
    }

    public Optional<String> find(String reference) {
        if (reference == null || reference.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(apiKeys.get(reference.trim()));
    }

    static Map<String, String> load(AgentProperties properties) {
        Map<String, String> keys = parseInline(properties.apiKeys());
        Map<String, String> fileKeys = parseFile(properties.apiKeyFile());
        if (!fileKeys.isEmpty()) {
            keys.putAll(fileKeys);
        }
        return Map.copyOf(keys);
    }

    static Map<String, String> parseInline(String value) {
        Map<String, String> keys = new LinkedHashMap<>();
        if (value == null || value.isBlank()) {
            return keys;
        }
        for (String pair : value.split(";")) {
            addPair(keys, pair);
        }
        return keys;
    }

    private static Map<String, String> parseFile(String file) {
        Map<String, String> keys = new LinkedHashMap<>();
        if (file == null || file.isBlank()) {
            return keys;
        }
        Path path = Path.of(file);
        if (!Files.isRegularFile(path)) {
            return keys;
        }
        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(path)) {
            properties.load(input);
        } catch (IOException ex) {
            log.warn("Failed to read API key file {}", file, ex);
            return keys;
        }
        properties.forEach((name, key) -> add(keys, String.valueOf(name), String.valueOf(key)));
        return keys;
    }

    private static void addPair(Map<String, String> keys, String pair) {
        if (pair == null || pair.isBlank()) {
            return;
        }
        int separator = pair.indexOf('=');
        if (separator < 1) {
            return;
        }
        add(keys, pair.substring(0, separator), pair.substring(separator + 1));
    }

    private static void add(Map<String, String> keys, String reference, String apiKey) {
        if (reference == null || apiKey == null) {
            return;
        }
        String trimmedReference = reference.trim();
        String trimmedApiKey = apiKey.trim();
        if (!trimmedReference.isBlank() && !trimmedApiKey.isBlank()) {
            keys.put(trimmedReference, trimmedApiKey);
        }
    }
}
