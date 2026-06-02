package cn.suhoan.asihavewritten.web;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import tools.jackson.databind.ObjectMapper;

import cn.suhoan.asihavewritten.log.IngestChannel;
import cn.suhoan.asihavewritten.log.LogIngestCommand;
import cn.suhoan.asihavewritten.log.LogIngestRequest;
import cn.suhoan.asihavewritten.log.LogIngestResponse;
import cn.suhoan.asihavewritten.log.LogIngestService;
import cn.suhoan.asihavewritten.log.LogSource;
import cn.suhoan.asihavewritten.log.LogSourceService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@Validated
@RestController
@RequestMapping("/api/logs")
public class IngestController {

    private final LogSourceService sourceService;
    private final LogIngestService ingestService;
    private final ObjectMapper objectMapper;

    public IngestController(LogSourceService sourceService, LogIngestService ingestService, ObjectMapper objectMapper) {
        this.sourceService = sourceService;
        this.ingestService = ingestService;
        this.objectMapper = objectMapper;
    }

    @PostMapping
    public ResponseEntity<LogIngestResponse> ingestOne(
            @RequestHeader("X-API-Key") String apiKey,
            @Valid @RequestBody LogIngestRequest request) {
        LogSource source = authenticate(apiKey);
        return accept(source, request, IngestChannel.HTTP)
                ? ResponseEntity.accepted().body(new LogIngestResponse(1))
                : ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(new LogIngestResponse(0));
    }

    @PostMapping("/batch")
    public ResponseEntity<LogIngestResponse> ingestBatch(
            @RequestHeader("X-API-Key") String apiKey,
            @Valid @RequestBody List<@Valid LogIngestRequest> requests) {
        LogSource source = authenticate(apiKey);
        long accepted = 0;
        for (LogIngestRequest request : requests) {
            if (!accept(source, request, IngestChannel.HTTP)) {
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(new LogIngestResponse(accepted));
            }
            accepted++;
        }
        return ResponseEntity.accepted().body(new LogIngestResponse(accepted));
    }

    @PostMapping(value = "/stream", consumes = {
            "application/x-ndjson",
            MediaType.APPLICATION_NDJSON_VALUE,
            MediaType.TEXT_PLAIN_VALUE
    })
    public ResponseEntity<LogIngestResponse> ingestStream(
            @RequestHeader("X-API-Key") String apiKey,
            HttpServletRequest request) throws IOException {
        LogSource source = authenticate(apiKey);
        long accepted = 0;
        try (BufferedReader reader = request.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                LogIngestRequest logRequest = objectMapper.readValue(line, LogIngestRequest.class);
                if (!accept(source, logRequest, IngestChannel.STREAM)) {
                    return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(new LogIngestResponse(accepted));
                }
                accepted++;
            }
        }
        return ResponseEntity.accepted().body(new LogIngestResponse(accepted));
    }

    private LogSource authenticate(String apiKey) {
        return sourceService.authenticate(apiKey)
                .orElseThrow(InvalidApiKeyException::new);
    }

    private boolean accept(LogSource source, LogIngestRequest request, IngestChannel channel) {
        return ingestService.accept(new LogIngestCommand(source, channel, request));
    }
}
