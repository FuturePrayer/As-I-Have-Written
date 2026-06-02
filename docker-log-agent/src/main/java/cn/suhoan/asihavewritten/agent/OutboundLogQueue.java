package cn.suhoan.asihavewritten.agent;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

@Component
public class OutboundLogQueue implements SmartLifecycle {

    private static final Logger log = LoggerFactory.getLogger(OutboundLogQueue.class);

    private final BlockingQueue<OutboundLogEvent> queue;
    private final LogSender sender;
    private final AgentProperties properties;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private Thread worker;

    public OutboundLogQueue(LogSender sender, AgentProperties properties) {
        this.sender = sender;
        this.properties = properties;
        this.queue = new ArrayBlockingQueue<>(properties.queueCapacity());
    }

    public boolean offer(String apiKey, LogPayload payload) {
        if (!queue.offer(new OutboundLogEvent(apiKey, payload))) {
            log.warn("Docker log agent queue is full; dropping log for service {}", payload.service());
            return false;
        }
        return true;
    }

    @Override
    public void start() {
        if (running.compareAndSet(false, true)) {
            worker = Thread.ofVirtual().name("aih-agent-sender").start(this::writeLoop);
        }
    }

    @Override
    public void stop() {
        running.set(false);
        if (worker != null) {
            worker.interrupt();
            try {
                worker.join(Math.max(1000, properties.requestTimeout().toMillis() + 500));
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }
        flushRemaining();
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }

    @Override
    public int getPhase() {
        return 0;
    }

    private void writeLoop() {
        List<OutboundLogEvent> batch = new ArrayList<>(properties.batchSize());
        while (running.get()) {
            try {
                batch.clear();
                long flushIntervalMillis = Math.max(1, properties.flushInterval().toMillis());
                OutboundLogEvent first = queue.poll(flushIntervalMillis, TimeUnit.MILLISECONDS);
                if (first == null) {
                    continue;
                }
                batch.add(first);
                collectUntilFullOrDue(batch, flushIntervalMillis);
                sendGrouped(batch);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                if (!batch.isEmpty()) {
                    sendGrouped(batch);
                    batch.clear();
                }
                return;
            } catch (RuntimeException ex) {
                log.warn("Failed to process Docker log batch", ex);
                batch.clear();
            }
        }
    }

    private void collectUntilFullOrDue(List<OutboundLogEvent> batch, long flushIntervalMillis) throws InterruptedException {
        queue.drainTo(batch, properties.batchSize() - batch.size());
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(flushIntervalMillis);
        while (batch.size() < properties.batchSize()) {
            long remainingNanos = deadline - System.nanoTime();
            if (remainingNanos <= 0) {
                return;
            }
            OutboundLogEvent next = queue.poll(remainingNanos, TimeUnit.NANOSECONDS);
            if (next == null) {
                return;
            }
            batch.add(next);
            queue.drainTo(batch, properties.batchSize() - batch.size());
        }
    }

    private void flushRemaining() {
        List<OutboundLogEvent> batch = new ArrayList<>(properties.batchSize());
        queue.drainTo(batch);
        if (!batch.isEmpty()) {
            sendGrouped(batch);
        }
    }

    void sendGrouped(List<OutboundLogEvent> batch) {
        Map<String, List<LogPayload>> grouped = new LinkedHashMap<>();
        for (OutboundLogEvent event : batch) {
            grouped.computeIfAbsent(event.apiKey(), ignored -> new ArrayList<>()).add(event.payload());
        }
        grouped.forEach(sender::send);
    }
}
