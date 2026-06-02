package cn.suhoan.asihavewritten.auth;

import org.springframework.stereotype.Service;

import cn.suhoan.asihavewritten.config.AdminProperties;

@Service
public class AdminAuthService {

    private final AdminProperties properties;

    public AdminAuthService(AdminProperties properties) {
        this.properties = properties;
    }

    public boolean matches(String username, String password) {
        return properties.username().equals(username) && properties.password().equals(password);
    }

    public String loginId() {
        return properties.username();
    }
}
