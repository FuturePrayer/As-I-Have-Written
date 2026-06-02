package cn.suhoan.asihavewritten.agent;

import java.time.Duration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;

@Configuration
public class DockerClientConfiguration {

    @Bean
    DockerClient dockerClient(AgentProperties properties) {
        DefaultDockerClientConfig.Builder configBuilder = DefaultDockerClientConfig.createDefaultConfigBuilder();
        if (properties.dockerHost() != null && !properties.dockerHost().isBlank()) {
            configBuilder.withDockerHost(properties.dockerHost().trim());
        }
        DefaultDockerClientConfig config = configBuilder.build();
        DockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
                .dockerHost(config.getDockerHost())
                .sslConfig(config.getSSLConfig())
                .connectionTimeout(Duration.ofSeconds(10))
                .responseTimeout(Duration.ZERO)
                .build();
        return DockerClientImpl.getInstance(config, httpClient);
    }
}
