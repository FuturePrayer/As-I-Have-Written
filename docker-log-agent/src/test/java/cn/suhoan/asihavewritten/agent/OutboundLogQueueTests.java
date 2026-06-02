package cn.suhoan.asihavewritten.agent;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class OutboundLogQueueTests {

    @Test
    void groupsPayloadsByApiKey() {
        CapturingLogSender sender = new CapturingLogSender();
        OutboundLogQueue queue = new OutboundLogQueue(sender, TestProperties.agentProperties(""));
        List<OutboundLogEvent> events = List.of(
                new OutboundLogEvent("key-a", payload("one")),
                new OutboundLogEvent("key-b", payload("two")),
                new OutboundLogEvent("key-a", payload("three")));

        queue.sendGrouped(events);

        assertThat(sender.calls).hasSize(2);
        assertThat(sender.calls.get(0).apiKey()).isEqualTo("key-a");
        assertThat(sender.calls.get(0).payloads()).extracting(LogPayload::message)
                .containsExactly("one", "three");
        assertThat(sender.calls.get(1).apiKey()).isEqualTo("key-b");
        assertThat(sender.calls.get(1).payloads()).extracting(LogPayload::message)
                .containsExactly("two");
    }

    @Test
    void dropsNewLogsWhenQueueIsFull() {
        CapturingLogSender sender = new CapturingLogSender();
        OutboundLogQueue queue = new OutboundLogQueue(sender, TestProperties.agentProperties("", 10, 1));

        assertThat(queue.offer("key-a", payload("one"))).isTrue();
        assertThat(queue.offer("key-a", payload("two"))).isFalse();

        queue.stop();

        assertThat(sender.calls).hasSize(1);
        assertThat(sender.calls.getFirst().payloads()).extracting(LogPayload::message)
                .containsExactly("one");
    }

    private LogPayload payload(String message) {
        return new LogPayload(
                Instant.parse("2026-06-02T12:00:00Z"),
                "service",
                "prod",
                LogLevel.INFO,
                null,
                null,
                message,
                Map.of());
    }

    private static final class CapturingLogSender extends LogSender {

        private final List<Call> calls = new ArrayList<>();

        private CapturingLogSender() {
            super();
        }

        @Override
        public void send(String apiKey, List<LogPayload> payloads) {
            calls.add(new Call(apiKey, List.copyOf(payloads)));
        }
    }

    private record Call(String apiKey, List<LogPayload> payloads) {
    }
}
