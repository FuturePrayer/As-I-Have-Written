package cn.suhoan.asihavewritten.log;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface LogSourceRepository extends MongoRepository<LogSource, String> {

    Optional<LogSource> findByNameAndEnabledTrue(String name);

    boolean existsByName(String name);

    boolean existsByServiceNameAndInstanceName(String serviceName, String instanceName);

    Optional<LogSource> findByServiceNameAndInstanceName(String serviceName, String instanceName);

    Optional<LogSource> findByApiKeyHash(String apiKeyHash);

    Optional<LogSource> findByApiKeyHashAndEnabledTrue(String apiKeyHash);

    java.util.List<LogSource> findAllByOrderByCreatedAtAsc();

    Optional<LogSource> findFirstByEnabledTrueOrderByCreatedAtAsc();

    long countByServiceName(String serviceName);
}
