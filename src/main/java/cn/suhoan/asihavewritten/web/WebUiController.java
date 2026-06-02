package cn.suhoan.asihavewritten.web;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.UriComponentsBuilder;

import cn.suhoan.asihavewritten.log.LogEntry;
import cn.suhoan.asihavewritten.log.LogLevel;
import cn.suhoan.asihavewritten.log.LogQuery;
import cn.suhoan.asihavewritten.log.LogQueryService;
import cn.suhoan.asihavewritten.log.LogService;
import cn.suhoan.asihavewritten.log.LogSourceService;

@Controller
public class WebUiController {

    private static final DateTimeFormatter INPUT_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

    private final LogQueryService queryService;
    private final LogSourceService sourceService;

    public WebUiController(LogQueryService queryService, LogSourceService sourceService) {
        this.queryService = queryService;
        this.sourceService = sourceService;
    }

    @GetMapping("/")
    public String index() {
        return "redirect:/ui/logs";
    }

    @GetMapping("/ui/login")
    public String login(@RequestParam(required = false) String redirect, Model model) {
        model.addAttribute("redirect", redirect);
        return "login";
    }

    @GetMapping("/ui/logs")
    public String logs(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false) String service,
            @RequestParam(required = false) String instanceName,
            @RequestParam(required = false) String environment,
            @RequestParam(required = false) LogLevel level,
            @RequestParam(required = false) String traceId,
            @RequestParam(required = false) String sourceId,
            @RequestParam(required = false) String q,
            @RequestParam Map<String, String> allParams,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            Model model) {
        List<LogService> services = sourceService.enabledServices();
        if (!services.isEmpty() && (service == null || service.isBlank())) {
            DateRange defaultRange = defaultRange();
            return "redirect:/ui/logs?service=" + services.getFirst().getName()
                    + "&from=" + formatDateTime(defaultRange.from())
                    + "&to=" + formatDateTime(defaultRange.to())
                    + "&size=" + size;
        }
        DateRange range = resolveRange(from, to);
        Map<String, String> metadataFilters = parseMetadataFilters(allParams);
        LogQuery logQuery = new LogQuery(
                range.from(),
                range.to(),
                service,
                instanceName,
                environment,
                level,
                traceId,
                sourceId,
                metadataFilters,
                parseTokens(q),
                page,
                size);
        Page<LogEntry> entries = queryService.search(logQuery);
        List<String> metadataKeys = mergeMetadataKeys(
                queryService.discoverMetadataKeys(service, range.from(), range.to()),
                metadataFilters);
        LogFilterValues filters = new LogFilterValues(
                formatDateTime(range.from()),
                formatDateTime(range.to()),
                service,
                instanceName,
                environment,
                level,
                traceId,
                sourceId,
                q,
                size);
        model.addAttribute("entries", entries);
        model.addAttribute("levels", LogLevel.values());
        model.addAttribute("services", services);
        model.addAttribute("metadataKeys", metadataKeys);
        model.addAttribute("metadataFilters", metadataFilters);
        model.addAttribute("filters", filters);
        model.addAttribute("previousPageUrl", logsPageUrl(entries.getNumber() - 1, filters, metadataFilters));
        model.addAttribute("nextPageUrl", logsPageUrl(entries.getNumber() + 1, filters, metadataFilters));
        return "logs";
    }

    @GetMapping("/ui/logs/{id}")
    public String detail(@PathVariable String id, Model model) {
        model.addAttribute("entry", queryService.getRequired(id));
        return "log-detail";
    }

    @GetMapping("/ui/cleanup")
    public String cleanup() {
        return "cleanup";
    }

    @PostMapping("/ui/cleanup")
    public String cleanup(@RequestParam String before, Model model) {
        Instant beforeInstant = parseDateTime(before);
        long deleted = queryService.deleteBefore(beforeInstant);
        sourceService.cleanupUnusedServices();
        model.addAttribute("deleted", deleted);
        return "cleanup";
    }

    @GetMapping("/ui/api-keys")
    public String apiKeys(Model model) {
        model.addAttribute("sources", sourceService.sources());
        model.addAttribute("services", sourceService.enabledServices());
        return "api-keys";
    }

    @PostMapping("/ui/api-keys")
    public String createApiKey(
            @RequestParam String serviceName,
            @RequestParam(required = false) String serviceDisplayName,
            @RequestParam String instanceName,
            Model model) {
        try {
            LogSourceService.CreatedLogSource created = sourceService.createSource(serviceName, serviceDisplayName, instanceName);
            model.addAttribute("createdApiKey", created.apiKey());
            model.addAttribute("createdSource", created.source());
        } catch (IllegalArgumentException ex) {
            model.addAttribute("error", ex.getMessage());
        }
        model.addAttribute("sources", sourceService.sources());
        model.addAttribute("services", sourceService.enabledServices());
        return "api-keys";
    }

    @PostMapping("/ui/api-keys/{id}/enable")
    public String enableApiKey(@PathVariable String id) {
        sourceService.setEnabled(id, true);
        return "redirect:/ui/api-keys";
    }

    @PostMapping("/ui/api-keys/{id}/disable")
    public String disableApiKey(@PathVariable String id) {
        sourceService.setEnabled(id, false);
        return "redirect:/ui/api-keys";
    }

    @PostMapping("/ui/api-keys/{id}/delete")
    public String deleteApiKey(@PathVariable String id) {
        sourceService.deleteSource(id);
        return "redirect:/ui/api-keys";
    }

    private Instant parseDateTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        LocalDateTime localDateTime = LocalDateTime.parse(value, INPUT_FORMATTER);
        return localDateTime.atZone(ZoneId.systemDefault()).toInstant();
    }

    private String formatDateTime(Instant value) {
        if (value == null) {
            return null;
        }
        return INPUT_FORMATTER.format(LocalDateTime.ofInstant(value, ZoneId.systemDefault()));
    }

    private DateRange resolveRange(String from, String to) {
        Instant parsedTo = parseDateTime(to);
        Instant resolvedTo = parsedTo == null ? Instant.now() : parsedTo;
        Instant parsedFrom = parseDateTime(from);
        Instant resolvedFrom = parsedFrom == null ? resolvedTo.minusSeconds(15 * 60L) : parsedFrom;
        return new DateRange(resolvedFrom, resolvedTo);
    }

    private DateRange defaultRange() {
        Instant to = Instant.now();
        return new DateRange(to.minusSeconds(15 * 60L), to);
    }

    private List<String> parseTokens(String q) {
        if (q == null || q.isBlank()) {
            return List.of();
        }
        return Arrays.stream(q.toLowerCase().split("\\s+"))
                .filter(token -> !token.isBlank())
                .distinct()
                .toList();
    }

    private Map<String, String> parseMetadataFilters(Map<String, String> params) {
        Map<String, String> filters = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (entry.getKey().startsWith("meta_") && entry.getValue() != null && !entry.getValue().isBlank()) {
                String key = entry.getKey().substring("meta_".length());
                if (key.matches("[A-Za-z0-9_.-]{1,64}")) {
                    filters.put(key, entry.getValue().trim());
                }
            }
        }
        return filters;
    }

    private List<String> mergeMetadataKeys(List<String> discoveredKeys, Map<String, String> activeFilters) {
        Set<String> keys = new LinkedHashSet<>(discoveredKeys);
        keys.addAll(activeFilters.keySet());
        return List.copyOf(keys);
    }

    private String logsPageUrl(int page, LogFilterValues filters, Map<String, String> metadataFilters) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/ui/logs")
                .queryParam("page", Math.max(page, 0))
                .queryParam("size", filters.size());
        addQueryParam(builder, "from", filters.from());
        addQueryParam(builder, "to", filters.to());
        addQueryParam(builder, "service", filters.service());
        addQueryParam(builder, "instanceName", filters.instanceName());
        addQueryParam(builder, "environment", filters.environment());
        if (filters.level() != null) {
            builder.queryParam("level", filters.level());
        }
        addQueryParam(builder, "traceId", filters.traceId());
        addQueryParam(builder, "sourceId", filters.sourceId());
        addQueryParam(builder, "q", filters.q());
        metadataFilters.forEach((key, value) -> addQueryParam(builder, "meta_" + key, value));
        return builder.build().encode().toUriString();
    }

    private void addQueryParam(UriComponentsBuilder builder, String name, String value) {
        if (value != null && !value.isBlank()) {
            builder.queryParam(name, value);
        }
    }

    public record LogFilterValues(
            String from,
            String to,
            String service,
            String instanceName,
            String environment,
            LogLevel level,
            String traceId,
            String sourceId,
            String q,
            int size) {
    }

    private record DateRange(Instant from, Instant to) {
    }
}
