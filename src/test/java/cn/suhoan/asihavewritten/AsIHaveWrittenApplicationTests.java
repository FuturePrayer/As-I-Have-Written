package cn.suhoan.asihavewritten;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static cn.suhoan.asihavewritten.TestMongoProperties.ENV_NAME;

@SpringBootTest
@EnabledIfEnvironmentVariable(named = ENV_NAME, matches = ".+")
class AsIHaveWrittenApplicationTests {

    @DynamicPropertySource
    static void mongoProperties(DynamicPropertyRegistry registry) {
        TestMongoProperties.register(registry);
    }

    @Test
    void contextLoads() {
    }

}
