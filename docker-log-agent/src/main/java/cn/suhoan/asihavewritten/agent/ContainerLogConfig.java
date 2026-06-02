package cn.suhoan.asihavewritten.agent;

import java.util.List;
import java.util.Map;

public record ContainerLogConfig(
        String containerId,
        String containerName,
        String image,
        String apiKeyRef,
        String service,
        String instance,
        String environment,
        String regex,
        List<String> regexMetadataGroups,
        String composeProject,
        String composeService,
        Map<String, String> safeLabels) {
}
