package cn.suhoan.asihavewritten.log;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Service;

@Service
public class LogQueryService {

    private static final int MAX_PAGE_SIZE = 200;

    private final MongoTemplate mongoTemplate;
    private final LogEntryRepository repository;

    public LogQueryService(MongoTemplate mongoTemplate, LogEntryRepository repository) {
        this.mongoTemplate = mongoTemplate;
        this.repository = repository;
    }

    public Page<LogEntry> search(LogQuery logQuery) {
        Query mongoQuery = buildQuery(logQuery);
        PageRequest pageRequest = PageRequest.of(
                Math.max(logQuery.page(), 0),
                Math.min(Math.max(logQuery.size(), 1), MAX_PAGE_SIZE),
                Sort.by(Sort.Order.desc("eventTime"), Sort.Order.desc("_id")));
        mongoQuery.with(pageRequest);
        List<LogEntry> entries = mongoTemplate.find(mongoQuery, LogEntry.class);
        Query countQuery = buildQuery(logQuery);
        return PageableExecutionUtils.getPage(
                entries,
                pageRequest,
                () -> mongoTemplate.count(countQuery, LogEntry.class));
    }

    public LogEntry getRequired(String id) {
        return repository.findById(id).orElseThrow(() -> new LogEntryNotFoundException(id));
    }

    public long deleteBefore(Instant before) {
        return repository.deleteByEventTimeBefore(before);
    }

    private Query buildQuery(LogQuery logQuery) {
        List<Criteria> criteria = new ArrayList<>();
        if (logQuery.from() != null || logQuery.to() != null) {
            Criteria time = Criteria.where("eventTime");
            if (logQuery.from() != null) {
                time = time.gte(logQuery.from());
            }
            if (logQuery.to() != null) {
                time = time.lte(logQuery.to());
            }
            criteria.add(time);
        }
        addString(criteria, "service", logQuery.service());
        addString(criteria, "instanceName", logQuery.instanceName());
        addString(criteria, "environment", logQuery.environment());
        addString(criteria, "traceId", logQuery.traceId());
        addString(criteria, "sourceId", logQuery.sourceId());
        if (logQuery.level() != null) {
            criteria.add(Criteria.where("level").is(logQuery.level()));
        }
        if (logQuery.tokens() != null && !logQuery.tokens().isEmpty()) {
            criteria.add(Criteria.where("tokens").all(logQuery.tokens()));
        }
        if (logQuery.metadataFilters() != null && !logQuery.metadataFilters().isEmpty()) {
            for (Map.Entry<String, String> entry : logQuery.metadataFilters().entrySet()) {
                if (isSafeMetadataKey(entry.getKey()) && entry.getValue() != null && !entry.getValue().isBlank()) {
                    criteria.add(Criteria.where("metadata." + entry.getKey()).is(entry.getValue().trim()));
                }
            }
        }
        Query query = new Query();
        if (!criteria.isEmpty()) {
            query.addCriteria(new Criteria().andOperator(criteria));
        }
        return query;
    }

    public List<String> discoverMetadataKeys(String service, Instant from, Instant to) {
        Query query = new Query();
        List<Criteria> criteria = new ArrayList<>();
        addString(criteria, "service", service);
        if (from != null || to != null) {
            Criteria time = Criteria.where("eventTime");
            if (from != null) {
                time = time.gte(from);
            }
            if (to != null) {
                time = time.lte(to);
            }
            criteria.add(time);
        }
        if (!criteria.isEmpty()) {
            query.addCriteria(new Criteria().andOperator(criteria));
        }
        query.with(Sort.by(Sort.Order.desc("eventTime"))).limit(200);
        query.fields().include("metadata");
        Set<String> keys = new LinkedHashSet<>();
        for (LogEntry entry : mongoTemplate.find(query, LogEntry.class)) {
            if (entry.getMetadata() == null) {
                continue;
            }
            for (Map.Entry<String, Object> metadata : entry.getMetadata().entrySet()) {
                if (isSafeMetadataKey(metadata.getKey()) && isFilterableValue(metadata.getValue())) {
                    keys.add(metadata.getKey());
                }
            }
        }
        return List.copyOf(keys);
    }

    private void addString(List<Criteria> criteria, String field, String value) {
        if (value != null && !value.isBlank()) {
            criteria.add(Criteria.where(field).is(value.trim()));
        }
    }

    private boolean isSafeMetadataKey(String key) {
        return key != null && key.matches("[A-Za-z0-9_.-]{1,64}");
    }

    private boolean isFilterableValue(Object value) {
        return value instanceof String || value instanceof Number || value instanceof Boolean;
    }
}
