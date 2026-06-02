package cn.suhoan.asihavewritten.auth;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import cn.suhoan.asihavewritten.config.SecurityProperties;

class ApiKeyCipherTests {

    @Test
    void encryptsAndDecryptsApiKey() {
        ApiKeyCipher cipher = new ApiKeyCipher(new SecurityProperties("test-secret"));

        String encrypted = cipher.encrypt("aih_secret");

        assertThat(encrypted).doesNotContain("aih_secret");
        assertThat(cipher.decrypt(encrypted)).isEqualTo("aih_secret");
    }
}
