package cn.suhoan.asihavewritten.log;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;

class TokenServiceTests {

    private final TokenService tokenService = new TokenService();

    @Test
    void tokenizesLatinNumbersAndChineseInApplication() {
        LogIngestRequest request = new LogIngestRequest(
                null,
                "Order-Service",
                "prod",
                LogLevel.ERROR,
                "trace-123",
                null,
                "支付失败 orderId=7788",
                Map.of("region", "cn-north"));

        assertThat(tokenService.tokenize(request))
                .contains("order-service", "prod", "trace-123", "7788", "支付失败", "支付", "失败", "region",
                        "cn-north");
    }

    @Test
    void deduplicatesTokens() {
        LogIngestRequest request = new LogIngestRequest(
                null,
                "billing",
                "billing",
                LogLevel.INFO,
                null,
                null,
                "billing billing",
                Map.of());

        assertThat(tokenService.tokenize(request))
                .containsExactly("billing");
    }
}
