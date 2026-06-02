package cn.suhoan.asihavewritten.agent;

import java.io.Closeable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.Event;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.core.command.EventsResultCallback;

@Component
public class DockerLogCollector implements SmartLifecycle {

    private static final Logger log = LoggerFactory.getLogger(DockerLogCollector.class);

    private final DockerClient dockerClient;
    private final ContainerConfigResolver configResolver;
    private final ApiKeyRegistry apiKeyRegistry;
    private final ContainerLogParser parser;
    private final OutboundLogQueue outboundQueue;
    private final Map<String, Closeable> logStreams = new ConcurrentHashMap<>();
    private final AtomicBoolean running = new AtomicBoolean(false);
    private EventsResultCallback eventCallback;

    public DockerLogCollector(
            DockerClient dockerClient,
            ContainerConfigResolver configResolver,
            ApiKeyRegistry apiKeyRegistry,
            ContainerLogParser parser,
            OutboundLogQueue outboundQueue) {
        this.dockerClient = dockerClient;
        this.configResolver = configResolver;
        this.apiKeyRegistry = apiKeyRegistry;
        this.parser = parser;
        this.outboundQueue = outboundQueue;
    }

    @Override
    public void start() {
        if (!running.compareAndSet(false, true)) {
            return;
        }
        startEventListener();
        startExistingContainers();
    }

    @Override
    public void stop() {
        running.set(false);
        if (eventCallback != null) {
            try {
                eventCallback.close();
            } catch (IOException ex) {
                log.debug("Failed to close Docker event stream", ex);
            }
        }
        logStreams.forEach((containerId, closeable) -> closeStream(containerId, closeable));
        logStreams.clear();
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }

    @Override
    public int getPhase() {
        return 100;
    }

    private void startExistingContainers() {
        try {
            List<Container> containers = dockerClient.listContainersCmd()
                    .withShowAll(false)
                    .exec();
            for (Container container : containers) {
                maybeStart(container);
            }
        } catch (RuntimeException ex) {
            log.warn("Failed to list Docker containers", ex);
        }
    }

    private void startEventListener() {
        eventCallback = new EventsResultCallback() {
            @Override
            public void onNext(Event event) {
                handleEvent(event);
            }

            @Override
            public void onError(Throwable throwable) {
                if (running.get()) {
                    log.warn("Docker event listener failed", throwable);
                }
                super.onError(throwable);
            }
        };
        try {
            dockerClient.eventsCmd().exec(eventCallback);
        } catch (RuntimeException ex) {
            log.warn("Failed to start Docker event listener", ex);
            try {
                eventCallback.close();
            } catch (IOException closeEx) {
                log.debug("Failed to close Docker event stream after startup failure", closeEx);
            }
            eventCallback = null;
        }
    }

    private void handleEvent(Event event) {
        if (event == null || event.getId() == null || event.getAction() == null) {
            return;
        }
        switch (event.getAction()) {
            case "start" -> inspectAndMaybeStart(event.getId());
            case "die", "destroy", "stop", "kill" -> closeContainerStream(event.getId());
            default -> {
            }
        }
    }

    private void inspectAndMaybeStart(String containerId) {
        try {
            InspectContainerResponse inspect = dockerClient.inspectContainerCmd(containerId).exec();
            ObservedContainer container = toContainer(inspect);
            maybeStart(container);
        } catch (RuntimeException ex) {
            log.debug("Failed to inspect started container {}", containerId, ex);
        }
    }

    private ObservedContainer toContainer(InspectContainerResponse inspect) {
        return new ObservedContainer(
                inspect.getId(),
                inspect.getName(),
                inspect.getConfig() == null ? null : inspect.getConfig().getImage(),
                inspect.getConfig() == null || inspect.getConfig().getLabels() == null
                        ? Map.of()
                        : inspect.getConfig().getLabels());
    }

    private void maybeStart(Container container) {
        if (container == null || container.getId() == null) {
            return;
        }
        maybeStart(new ObservedContainer(
                container.getId(),
                container.getNames() == null || container.getNames().length == 0 ? null : container.getNames()[0],
                container.getImage(),
                container.getLabels()));
    }

    private void maybeStart(ObservedContainer container) {
        if (container == null || container.id() == null || logStreams.containsKey(container.id())) {
            return;
        }
        Optional<ContainerLogConfig> config = configResolver.resolve(container);
        if (config.isEmpty()) {
            return;
        }
        Optional<String> apiKey = apiKeyRegistry.find(config.get().apiKeyRef());
        if (apiKey.isEmpty()) {
            log.warn("Container {} enabled log forwarding but API key ref '{}' is not configured",
                    config.get().containerName(), config.get().apiKeyRef());
            return;
        }
        startLogStream(config.get(), apiKey.get());
    }

    private void startLogStream(ContainerLogConfig config, String apiKey) {
        ResultCallback.Adapter<Frame> callback = new ResultCallback.Adapter<>() {
            @Override
            public void onNext(Frame frame) {
                handleFrame(config, apiKey, frame);
            }

            @Override
            public void onError(Throwable throwable) {
                if (running.get()) {
                    log.warn("Docker log stream failed for container {}", config.containerName(), throwable);
                }
                closeContainerStream(config.containerId());
                super.onError(throwable);
            }

            @Override
            public void onComplete() {
                closeContainerStream(config.containerId());
                super.onComplete();
            }
        };
        logStreams.put(config.containerId(), callback);
        try {
            dockerClient.logContainerCmd(config.containerId())
                    .withStdOut(true)
                    .withStdErr(true)
                    .withFollowStream(true)
                    .withSince((int) Instant.now().getEpochSecond())
                    .exec(callback);
            log.info("Started Docker log forwarding for container {} ({})",
                    config.containerName(), shortId(config.containerId()));
        } catch (RuntimeException ex) {
            logStreams.remove(config.containerId());
            closeStream(config.containerId(), callback);
            log.warn("Failed to start Docker log forwarding for container {}", config.containerName(), ex);
        }
    }

    private void handleFrame(ContainerLogConfig config, String apiKey, Frame frame) {
        if (frame == null || frame.getPayload() == null) {
            return;
        }
        DockerStream stream = frame.getStreamType() != null && frame.getStreamType().name().equals("STDERR")
                ? DockerStream.STDERR
                : DockerStream.STDOUT;
        String text = new String(frame.getPayload(), StandardCharsets.UTF_8);
        for (String line : text.split("\\R")) {
            if (line.isBlank()) {
                continue;
            }
            outboundQueue.offer(apiKey, parser.parse(config, stream, Instant.now(), line));
        }
    }

    private void closeContainerStream(String containerId) {
        Closeable closeable = logStreams.remove(containerId);
        if (closeable != null) {
            closeStream(containerId, closeable);
        }
    }

    private void closeStream(String containerId, Closeable closeable) {
        try {
            closeable.close();
        } catch (IOException ex) {
            log.debug("Failed to close Docker log stream for {}", containerId, ex);
        }
    }

    private String shortId(String containerId) {
        if (containerId == null || containerId.length() <= 12) {
            return String.valueOf(containerId);
        }
        return containerId.substring(0, 12);
    }
}
