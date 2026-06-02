package cn.suhoan.asihavewritten;

import org.springframework.test.context.DynamicPropertyRegistry;

public final class TestMongoProperties {

    public static final String ENV_NAME = "AIH_TEST_MONGODB_URI";

    private TestMongoProperties() {
    }

    public static void register(DynamicPropertyRegistry registry) {
        registry.add("spring.mongodb.uri", () -> System.getenv(ENV_NAME));
        registry.add("aih.security.api-key-encryption-key", () -> "test-api-key-encryption-secret");
    }
}
