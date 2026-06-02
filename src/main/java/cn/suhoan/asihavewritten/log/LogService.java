package cn.suhoan.asihavewritten.log;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("log_services")
public class LogService {

    @Id
    private String id;

    @Indexed(unique = true)
    private String name;

    private String displayName;

    @Indexed
    private long sortOrder;

    private boolean enabled;
    private Instant createdAt;

    public LogService() {
    }

    public LogService(String name, String displayName, long sortOrder, boolean enabled, Instant createdAt) {
        this.name = name;
        this.displayName = displayName;
        this.sortOrder = sortOrder;
        this.enabled = enabled;
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public long getSortOrder() {
        return sortOrder;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
