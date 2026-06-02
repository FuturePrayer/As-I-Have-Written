package cn.suhoan.asihavewritten.log;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import cn.suhoan.asihavewritten.auth.ApiKeyCipher;
import cn.suhoan.asihavewritten.auth.ApiKeyGenerator;
import cn.suhoan.asihavewritten.auth.ApiKeyHasher;
import cn.suhoan.asihavewritten.config.IngestProperties;

@Service
public class LogSourceService {

    private final LogSourceRepository repository;
    private final LogServiceRepository serviceRepository;
    private final LogEntryRepository entryRepository;
    private final ApiKeyHasher apiKeyHasher;
    private final ApiKeyCipher apiKeyCipher;
    private final ApiKeyGenerator apiKeyGenerator;
    private final IngestProperties ingestProperties;

    public LogSourceService(LogSourceRepository repository, LogServiceRepository serviceRepository,
            LogEntryRepository entryRepository,
            ApiKeyHasher apiKeyHasher, ApiKeyCipher apiKeyCipher, ApiKeyGenerator apiKeyGenerator,
            IngestProperties ingestProperties) {
        this.repository = repository;
        this.serviceRepository = serviceRepository;
        this.entryRepository = entryRepository;
        this.apiKeyHasher = apiKeyHasher;
        this.apiKeyCipher = apiKeyCipher;
        this.apiKeyGenerator = apiKeyGenerator;
        this.ingestProperties = ingestProperties;
    }

    public void ensureDefaultSource() {
        String serviceName = normalizeName(ingestProperties.defaultServiceName());
        String instanceName = normalizeName(ingestProperties.defaultInstanceName());
        String apiKey = normalizeName(ingestProperties.defaultApiKey());
        String apiKeyHash = apiKeyHasher.hash(apiKey);
        ensureService(serviceName, ingestProperties.defaultServiceDisplayName());
        if (repository.findByServiceNameAndInstanceName(serviceName, instanceName).isPresent()) {
            return;
        }
        Optional<LogSource> existingSource = repository.findByApiKeyHash(apiKeyHash);
        if (existingSource.isPresent()) {
            LogSource source = existingSource.get();
            boolean changed = fillDefaultSourceFields(source, serviceName, instanceName, apiKey);
            if (changed) {
                repository.save(source);
            }
            return;
        }
        String name = sourceName(serviceName, instanceName);
        repository.save(new LogSource(
                name,
                serviceName,
                instanceName,
                apiKeyHash,
                apiKeyCipher.encrypt(apiKey),
                true,
                Instant.now()));
    }

    public Optional<LogSource> authenticate(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            return Optional.empty();
        }
        return repository.findByApiKeyHashAndEnabledTrue(apiKeyHasher.hash(apiKey));
    }

    public Optional<LogSource> firstEnabledSource() {
        return repository.findFirstByEnabledTrueOrderByCreatedAtAsc();
    }

    public List<LogService> enabledServices() {
        return serviceRepository.findByEnabledTrueOrderBySortOrderAscCreatedAtAsc();
    }

    public List<LogSourceView> sources() {
        Map<String, String> serviceDisplayNames = serviceRepository.findAll().stream()
                .collect(Collectors.toMap(LogService::getName, LogService::getDisplayName, (left, right) -> left));
        return repository.findAllByOrderByCreatedAtAsc().stream()
                .map(source -> {
                    String apiKey = apiKeyCipher.decrypt(source.getEncryptedApiKey());
                    return new LogSourceView(
                            source.getId(),
                            source.getName(),
                            source.getServiceName(),
                            serviceDisplayNames.getOrDefault(source.getServiceName(), source.getServiceName()),
                            source.getInstanceName(),
                            apiKey,
                            maskApiKey(apiKey),
                            source.isEnabled(),
                            source.getCreatedAt());
                })
                .toList();
    }

    public CreatedLogSource createSource(String serviceName, String serviceDisplayName, String instanceName) {
        String normalizedServiceName = normalizeName(serviceName);
        String normalizedInstanceName = normalizeName(instanceName);
        ensureService(normalizedServiceName,
                StringUtils.hasText(serviceDisplayName) ? serviceDisplayName.trim() : normalizedServiceName);
        if (repository.existsByServiceNameAndInstanceName(normalizedServiceName, normalizedInstanceName)) {
            throw new IllegalArgumentException("API key already exists for service and instance");
        }
        String apiKey = apiKeyGenerator.generate();
        LogSource saved = repository.save(new LogSource(
                sourceName(normalizedServiceName, normalizedInstanceName),
                normalizedServiceName,
                normalizedInstanceName,
                apiKeyHasher.hash(apiKey),
                apiKeyCipher.encrypt(apiKey),
                true,
                Instant.now()));
        return new CreatedLogSource(saved, apiKey);
    }

    public void setEnabled(String id, boolean enabled) {
        LogSource source = repository.findById(id).orElseThrow(() -> new IllegalArgumentException("API key not found"));
        source.setEnabled(enabled);
        repository.save(source);
    }

    public void deleteSource(String id) {
        LogSource source = repository.findById(id).orElseThrow(() -> new IllegalArgumentException("API key not found"));
        repository.delete(source);
    }

    public void cleanupUnusedServices() {
        serviceRepository.findAll().forEach(service -> cleanupServiceIfUnused(service.getName()));
    }

    private void ensureService(String serviceName, String displayName) {
        if (!serviceRepository.existsByName(serviceName)) {
            long nextSortOrder = serviceRepository.findAll().stream()
                    .map(LogService::getSortOrder)
                    .max(Comparator.naturalOrder())
                    .orElse(0L) + 1L;
            serviceRepository.save(new LogService(serviceName, displayName, nextSortOrder, true, Instant.now()));
        }
    }

    private String sourceName(String serviceName, String instanceName) {
        return serviceName + "/" + instanceName;
    }

    private String maskApiKey(String apiKey) {
        if (!StringUtils.hasText(apiKey)) {
            return "";
        }
        int prefixLength = Math.min(4, apiKey.length());
        int suffixLength = apiKey.length() > 7 ? 3 : Math.max(0, apiKey.length() - prefixLength);
        return apiKey.substring(0, prefixLength)
                + "••••••••••••••••••••"
                + apiKey.substring(apiKey.length() - suffixLength);
    }

    private void cleanupServiceIfUnused(String serviceName) {
        if (!StringUtils.hasText(serviceName)) {
            return;
        }
        if (repository.countByServiceName(serviceName) == 0 && entryRepository.countByService(serviceName) == 0) {
            serviceRepository.findByName(serviceName).ifPresent(serviceRepository::delete);
        }
    }

    private boolean fillDefaultSourceFields(LogSource source, String serviceName, String instanceName, String apiKey) {
        boolean changed = false;
        if (!StringUtils.hasText(source.getName())) {
            source.setName(sourceName(serviceName, instanceName));
            changed = true;
        }
        if (!StringUtils.hasText(source.getServiceName())) {
            source.setServiceName(serviceName);
            changed = true;
        }
        if (!StringUtils.hasText(source.getInstanceName())) {
            source.setInstanceName(instanceName);
            changed = true;
        }
        if (!StringUtils.hasText(source.getEncryptedApiKey())) {
            source.setEncryptedApiKey(apiKeyCipher.encrypt(apiKey));
            changed = true;
        }
        if (source.getCreatedAt() == null) {
            source.setCreatedAt(Instant.now());
            changed = true;
        }
        return changed;
    }

    private String normalizeName(String value) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException("Name must not be blank");
        }
        return value.trim();
    }

    public record CreatedLogSource(LogSource source, String apiKey) {
    }

    public record LogSourceView(
            String id,
            String name,
            String serviceName,
            String serviceDisplayName,
            String instanceName,
            String apiKey,
            String maskedApiKey,
            boolean enabled,
            Instant createdAt) {
    }
}
