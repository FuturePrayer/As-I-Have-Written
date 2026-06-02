package cn.suhoan.asihavewritten.agent;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ContainerLogParser {

    private static final Logger log = LoggerFactory.getLogger(ContainerLogParser.class);

    public LogPayload parse(ContainerLogConfig config, DockerStream stream, Instant collectedAt, String line) {
        ParsedLine parsed = parseLine(config, stream, collectedAt, line);
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("containerId", config.containerId());
        metadata.put("containerName", config.containerName());
        metadata.put("image", config.image());
        metadata.put("stream", stream.name().toLowerCase());
        putIfNotBlank(metadata, "instance", config.instance());
        putIfNotBlank(metadata, "composeProject", config.composeProject());
        putIfNotBlank(metadata, "composeService", config.composeService());
        if (!config.safeLabels().isEmpty()) {
            metadata.put("dockerLabels", config.safeLabels());
        }
        parsed.metadata().forEach(metadata::putIfAbsent);

        return new LogPayload(
                parsed.eventTime(),
                config.service(),
                config.environment(),
                parsed.level(),
                parsed.traceId(),
                parsed.spanId(),
                parsed.message(),
                metadata);
    }

    private ParsedLine parseLine(ContainerLogConfig config, DockerStream stream, Instant collectedAt, String line) {
        String regex = config.regex();
        if (regex == null || regex.isBlank()) {
            return fallback(stream, collectedAt, line);
        }
        Matcher matcher;
        try {
            matcher = Pattern.compile(regex).matcher(line);
        } catch (PatternSyntaxException ex) {
            log.warn("Invalid log regex for container {}: {}", config.containerName(), ex.getMessage());
            return fallback(stream, collectedAt, line);
        }
        if (!matcher.matches()) {
            return fallback(stream, collectedAt, line);
        }

        Instant eventTime = parseTime(group(matcher, "time"), collectedAt);
        LogLevel level = LogLevel.from(group(matcher, "level"), stream.defaultLevel());
        String traceId = blankToNull(group(matcher, "traceId"));
        String spanId = blankToNull(group(matcher, "spanId"));
        String message = blankToNull(group(matcher, "message"));
        if (message == null) {
            message = line;
        }

        Map<String, Object> metadata = new LinkedHashMap<>();
        for (String group : config.regexMetadataGroups()) {
            String value = blankToNull(group(matcher, group));
            if (value != null) {
                metadata.put(group, value);
            }
        }
        return new ParsedLine(eventTime, level, traceId, spanId, message, metadata);
    }

    private ParsedLine fallback(DockerStream stream, Instant collectedAt, String line) {
        return new ParsedLine(collectedAt, stream.defaultLevel(), null, null, line, Map.of());
    }

    private Instant parseTime(String value, Instant fallback) {
        String time = blankToNull(value);
        if (time == null) {
            return fallback;
        }
        try {
            return Instant.parse(time);
        } catch (DateTimeParseException ex) {
            return fallback;
        }
    }

    private String group(Matcher matcher, String name) {
        try {
            return matcher.group(name);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return null;
        }
    }

    private void putIfNotBlank(Map<String, Object> metadata, String key, String value) {
        String trimmed = blankToNull(value);
        if (trimmed != null) {
            metadata.put(key, trimmed);
        }
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private record ParsedLine(
            Instant eventTime,
            LogLevel level,
            String traceId,
            String spanId,
            String message,
            Map<String, Object> metadata) {
    }
}
