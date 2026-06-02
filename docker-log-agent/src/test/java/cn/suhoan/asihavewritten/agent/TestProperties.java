package cn.suhoan.asihavewritten.agent;

import java.time.Duration;

final class TestProperties {

    private TestProperties() {
    }

    static AgentProperties agentProperties(String inlineKeys) {
        return agentProperties(inlineKeys, 500, 1000);
    }

    static AgentProperties agentProperties(String inlineKeys, int batchSize, int queueCapacity) {
        return new AgentProperties(
                "http://localhost:25091/api/logs/batch",
                "",
                inlineKeys,
                "local",
                "",
                "",
                batchSize,
                queueCapacity,
                Duration.ofSeconds(1),
                Duration.ofSeconds(5),
                "");
    }
}
