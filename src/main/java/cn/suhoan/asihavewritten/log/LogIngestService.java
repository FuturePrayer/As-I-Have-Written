package cn.suhoan.asihavewritten.log;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Service;

import cn.suhoan.asihavewritten.config.IngestProperties;

@Service
public class LogIngestService implements SmartLifecycle {

    private static final Logger log = LoggerFactory.getLogger(LogIngestService.class);

    private final BlockingQueue<LogIngestCommand> queue;
    private final LogEntryRepository repository;
    private final TokenService tokenService;
    private final IngestProperties properties;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private Thread worker;

    public LogIngestService(LogEntryRepository repository, TokenService tokenService, IngestProperties properties) {
        this.repository = repository;
        this.tokenService = tokenService;
        this.properties = properties;
        this.queue = new ArrayBlockingQueue<>(properties.queueCapacity());
    }

    public boolean accept(LogIngestCommand command) {
        return queue.offer(command);
    }

    public int queuedCount() {
        return queue.size();
    }

    @Override
    public void start() {
        if (running.compareAndSet(false, true)) {
            worker = Thread.ofVirtual().name("log-ingest-writer").start(this::writeLoop);
        }
    }

    @Override
    public void stop() {
        running.set(false);
        if (worker != null) {
            worker.interrupt();
        }
        flushRemaining();
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }

    private void writeLoop() {
        List<LogIngestCommand> batch = new ArrayList<>(properties.batchSize());
        while (running.get()) {
            try {
                LogIngestCommand first = queue.poll(properties.flushInterval().toMillis(), TimeUnit.MILLISECONDS);
                if (first != null) {
                    batch.add(first);
                    queue.drainTo(batch, properties.batchSize() - batch.size());
                    persist(batch);
                    batch.clear();
                }
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                return;
            } catch (RuntimeException ex) {
                log.error("Failed to persist log batch", ex);
                batch.clear();
            }
        }
    }

    private void flushRemaining() {
        List<LogIngestCommand> batch = new ArrayList<>(properties.batchSize());
        queue.drainTo(batch);
        if (!batch.isEmpty()) {
            persist(batch);
        }
    }

    private void persist(List<LogIngestCommand> batch) {
        Instant receivedAt = Instant.now();
        List<LogEntry> entries = batch.stream()
                .map(command -> toEntry(command, receivedAt))
                .toList();
        repository.saveAll(entries);
    }

    private LogEntry toEntry(LogIngestCommand command, Instant receivedAt) {
        LogIngestRequest request = command.request();
        Map<String, Object> metadata = request.metadata() == null ? Map.of() : request.metadata();
        return new LogEntry(
                request.eventTime() == null ? receivedAt : request.eventTime(),
                receivedAt,
                command.source().getId(),
                command.source().getServiceName(),
                command.source().getInstanceName(),
                request.environment().trim(),
                request.level(),
                blankToNull(request.traceId()),
                blankToNull(request.spanId()),
                request.message(),
                metadata,
                tokenService.tokenize(request),
                command.channel());
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
