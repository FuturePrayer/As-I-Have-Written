package cn.suhoan.asihavewritten.log;

import java.time.Instant;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface LogEntryRepository extends MongoRepository<LogEntry, String> {

    long deleteByEventTimeBefore(Instant before);

    long countByService(String service);
}
