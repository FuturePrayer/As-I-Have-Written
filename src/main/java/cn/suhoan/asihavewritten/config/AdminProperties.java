package cn.suhoan.asihavewritten.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "aih.admin")
public record AdminProperties(String username, String password) {
}
