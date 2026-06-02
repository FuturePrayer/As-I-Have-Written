package cn.suhoan.asihavewritten.auth;

import java.security.SecureRandom;
import java.util.Base64;

import org.springframework.stereotype.Component;

@Component
public class ApiKeyGenerator {

    private final SecureRandom secureRandom = new SecureRandom();

    public String generate() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return "aih_" + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
