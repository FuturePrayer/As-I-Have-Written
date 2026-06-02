package cn.suhoan.asihavewritten.auth;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ApiKeyHasherTests {

    private final ApiKeyHasher hasher = new ApiKeyHasher();

    @Test
    void matchesOriginalApiKeyOnly() {
        String hash = hasher.hash("secret-key");

        assertThat(hasher.matches("secret-key", hash)).isTrue();
        assertThat(hasher.matches("other-key", hash)).isFalse();
    }
}
