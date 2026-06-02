package cn.suhoan.asihavewritten.log;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("log_entries")
@CompoundIndex(name = "query_sort_idx", def = "{'eventTime': -1, '_id': -1}")
public class LogEntry {

    @Id
    private String id;

    @Indexed
    private Instant eventTime;

    @Indexed
    private Instant receivedAt;

    @Indexed
    private String sourceId;

    @Indexed
    private String service;

    @Indexed
    private String instanceName;

    @Indexed
    private String environment;

    @Indexed
    private LogLevel level;

    @Indexed
    private String traceId;

    private String spanId;
    private String message;
    private Map<String, Object> metadata;

    @Indexed
    private List<String> tokens;

    private IngestChannel ingestChannel;

    public LogEntry() {
    }

    public LogEntry(Instant eventTime, Instant receivedAt, String sourceId, String service, String instanceName, String environment,
            LogLevel level, String traceId, String spanId, String message, Map<String, Object> metadata,
            List<String> tokens, IngestChannel ingestChannel) {
        this.eventTime = eventTime;
        this.receivedAt = receivedAt;
        this.sourceId = sourceId;
        this.service = service;
        this.instanceName = instanceName;
        this.environment = environment;
        this.level = level;
        this.traceId = traceId;
        this.spanId = spanId;
        this.message = message;
        this.metadata = metadata;
        this.tokens = tokens;
        this.ingestChannel = ingestChannel;
    }

    public String getId() {
        return id;
    }

    public Instant getEventTime() {
        return eventTime;
    }

    public Instant getReceivedAt() {
        return receivedAt;
    }

    public String getSourceId() {
        return sourceId;
    }

    public String getService() {
        return service;
    }

    public String getInstanceName() {
        return instanceName;
    }

    public String getEnvironment() {
        return environment;
    }

    public LogLevel getLevel() {
        return level;
    }

    public String getTraceId() {
        return traceId;
    }

    public String getSpanId() {
        return spanId;
    }

    public String getMessage() {
        return message;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public List<String> getTokens() {
        return tokens;
    }

    public IngestChannel getIngestChannel() {
        return ingestChannel;
    }
}
