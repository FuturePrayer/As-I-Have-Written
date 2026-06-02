package cn.suhoan.asihavewritten.log;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface LogServiceRepository extends MongoRepository<LogService, String> {

    boolean existsByName(String name);

    Optional<LogService> findByName(String name);

    List<LogService> findByEnabledTrueOrderBySortOrderAscCreatedAtAsc();
}
