package cn.suhoan.asihavewritten.agent;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;

class ContainerConfigResolverTests {

    private final ContainerConfigResolver resolver = new ContainerConfigResolver(TestProperties.agentProperties(""));

    @Test
    void ignoresContainersWithoutOptInLabel() {
        ObservedContainer container = new ObservedContainer(
                "abc",
                "/app",
                "example/app:latest",
                Map.of(ContainerConfigResolver.LABEL_API_KEY_REF, "app"));

        assertThat(resolver.resolve(container)).isEmpty();
    }

    @Test
    void ignoresAgentSelfContainer() {
        ObservedContainer container = new ObservedContainer(
                "abc",
                "/agent",
                "agent:latest",
                Map.of(
                        ContainerConfigResolver.LABEL_ENABLED, "true",
                        ContainerConfigResolver.LABEL_AGENT_SELF, "true",
                        ContainerConfigResolver.LABEL_API_KEY_REF, "agent"));

        assertThat(resolver.resolve(container)).isEmpty();
    }

    @Test
    void resolvesLabelsAndComposeDefaults() {
        ObservedContainer container = new ObservedContainer(
                "abc123",
                "/project-payment-1",
                "example/payment:latest",
                Map.of(
                        ContainerConfigResolver.LABEL_ENABLED, "true",
                        ContainerConfigResolver.LABEL_API_KEY_REF, "payment-key",
                        ContainerConfigResolver.LABEL_COMPOSE_PROJECT, "shop",
                        ContainerConfigResolver.LABEL_COMPOSE_SERVICE, "payment",
                        ContainerConfigResolver.LABEL_ENVIRONMENT, "prod",
                        ContainerConfigResolver.LABEL_REGEX_METADATA_GROUPS, "tenant,user_id",
                        "unsafe.secret", "hidden",
                        "safe.label", "visible"));

        ContainerLogConfig config = resolver.resolve(container).orElseThrow();

        assertThat(config.containerId()).isEqualTo("abc123");
        assertThat(config.containerName()).isEqualTo("project-payment-1");
        assertThat(config.apiKeyRef()).isEqualTo("payment-key");
        assertThat(config.service()).isEqualTo("payment");
        assertThat(config.instance()).isEqualTo("project-payment-1");
        assertThat(config.environment()).isEqualTo("prod");
        assertThat(config.regexMetadataGroups()).containsExactly("tenant", "user_id");
        assertThat(config.safeLabels()).containsEntry("safe.label", "visible");
        assertThat(config.safeLabels()).doesNotContainKey(ContainerConfigResolver.LABEL_API_KEY_REF);
        assertThat(config.safeLabels()).doesNotContainKey("unsafe.secret");
    }
}
