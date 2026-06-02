package cn.suhoan.asihavewritten.agent;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

@Component
public class ContainerConfigResolver {

    static final String LABEL_ENABLED = "aih.logs.enabled";
    static final String LABEL_API_KEY_REF = "aih.logs.api-key-ref";
    static final String LABEL_SERVICE = "aih.logs.service";
    static final String LABEL_INSTANCE = "aih.logs.instance";
    static final String LABEL_ENVIRONMENT = "aih.logs.environment";
    static final String LABEL_REGEX = "aih.logs.regex";
    static final String LABEL_REGEX_METADATA_GROUPS = "aih.logs.regex-metadata-groups";
    static final String LABEL_COMPOSE_PROJECT = "com.docker.compose.project";
    static final String LABEL_COMPOSE_SERVICE = "com.docker.compose.service";
    static final String LABEL_AGENT_SELF = "aih.logs.agent";

    private static final Pattern SAFE_LABEL_NAME = Pattern.compile("[A-Za-z0-9_.-]{1,96}");
    private static final int MAX_LABEL_VALUE_LENGTH = 512;

    private final AgentProperties properties;

    public ContainerConfigResolver(AgentProperties properties) {
        this.properties = properties;
    }

    public Optional<ContainerLogConfig> resolve(ObservedContainer container) {
        Map<String, String> labels = container.labels() == null ? Map.of() : container.labels();
        if (isTrue(labels.get(LABEL_AGENT_SELF)) || !isTrue(labels.get(LABEL_ENABLED))) {
            return Optional.empty();
        }

        String apiKeyRef = trimToNull(labels.get(LABEL_API_KEY_REF));
        if (apiKeyRef == null) {
            return Optional.empty();
        }

        String composeProject = trimToNull(labels.get(LABEL_COMPOSE_PROJECT));
        String composeService = trimToNull(labels.get(LABEL_COMPOSE_SERVICE));
        String containerName = normalizeName(container.name());
        String service = firstNonBlank(labels.get(LABEL_SERVICE), composeService, containerName);
        String instance = firstNonBlank(labels.get(LABEL_INSTANCE), containerName);
        String environment = firstNonBlank(labels.get(LABEL_ENVIRONMENT), properties.defaultEnvironment());
        String regex = firstNonBlank(labels.get(LABEL_REGEX), properties.logRegex());
        List<String> metadataGroups = parseList(firstNonBlank(
                labels.get(LABEL_REGEX_METADATA_GROUPS),
                properties.logRegexMetadataGroups()));

        return Optional.of(new ContainerLogConfig(
                container.id(),
                containerName,
                container.image(),
                apiKeyRef,
                service,
                instance,
                environment,
                regex,
                metadataGroups,
                composeProject,
                composeService,
                safeLabels(labels)));
    }

    private boolean isTrue(String value) {
        return value != null && value.trim().equalsIgnoreCase("true");
    }

    private String normalizeName(String value) {
        if (value == null || value.isBlank()) {
            return "unknown-container";
        }
        String name = value.trim();
        while (name.startsWith("/")) {
            name = name.substring(1);
        }
        return name.isBlank() ? "unknown-container" : name;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            String trimmed = trimToNull(value);
            if (trimmed != null) {
                return trimmed;
            }
        }
        return null;
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private List<String> parseList(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .filter(item -> item.matches("[A-Za-z][A-Za-z0-9_]*"))
                .distinct()
                .toList();
    }

    private Map<String, String> safeLabels(Map<String, String> labels) {
        Map<String, String> safe = new LinkedHashMap<>();
        labels.entrySet().stream()
                .filter(entry -> SAFE_LABEL_NAME.matcher(entry.getKey()).matches())
                .filter(entry -> !entry.getKey().equals(LABEL_API_KEY_REF))
                .filter(entry -> !entry.getKey().toLowerCase().contains("key"))
                .filter(entry -> !entry.getKey().toLowerCase().contains("secret"))
                .forEach(entry -> safe.put(entry.getKey(), truncate(entry.getValue())));
        return safe;
    }

    private String truncate(String value) {
        if (value == null) {
            return "";
        }
        return value.length() <= MAX_LABEL_VALUE_LENGTH
                ? value
                : value.substring(0, MAX_LABEL_VALUE_LENGTH);
    }
}
