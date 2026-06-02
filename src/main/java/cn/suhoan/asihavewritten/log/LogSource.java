package cn.suhoan.asihavewritten.log;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("log_sources")
@CompoundIndex(name = "service_instance_idx", def = "{'serviceName': 1, 'instanceName': 1}", unique = true)
public class LogSource {

    @Id
    private String id;

    @Indexed(unique = true)
    private String name;

    @Indexed
    private String serviceName;

    @Indexed
    private String instanceName;

    @Indexed(unique = true)
    private String apiKeyHash;

    private String encryptedApiKey;
    private boolean enabled;
    private Instant createdAt;

    public LogSource() {
    }

    public LogSource(String name, String serviceName, String instanceName, String apiKeyHash, String encryptedApiKey,
            boolean enabled, Instant createdAt) {
        this.name = name;
        this.serviceName = serviceName;
        this.instanceName = instanceName;
        this.apiKeyHash = apiKeyHash;
        this.encryptedApiKey = encryptedApiKey;
        this.enabled = enabled;
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getInstanceName() {
        return instanceName;
    }

    public void setInstanceName(String instanceName) {
        this.instanceName = instanceName;
    }

    public String getApiKeyHash() {
        return apiKeyHash;
    }

    public void setApiKeyHash(String apiKeyHash) {
        this.apiKeyHash = apiKeyHash;
    }

    public String getEncryptedApiKey() {
        return encryptedApiKey;
    }

    public void setEncryptedApiKey(String encryptedApiKey) {
        this.encryptedApiKey = encryptedApiKey;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
