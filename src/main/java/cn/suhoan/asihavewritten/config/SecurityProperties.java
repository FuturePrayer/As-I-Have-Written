package cn.suhoan.asihavewritten.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "aih.security")
public record SecurityProperties(String apiKeyEncryptionKey) {
}
