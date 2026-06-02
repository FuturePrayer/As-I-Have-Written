package cn.suhoan.asihavewritten.agent;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import tools.jackson.databind.ObjectMapper;

@Component
public class LogSender {

    private static final Logger log = LoggerFactory.getLogger(LogSender.class);

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final URI endpoint;
    private final Duration requestTimeout;

    protected LogSender() {
        this.objectMapper = null;
        this.httpClient = null;
        this.endpoint = null;
        this.requestTimeout = Duration.ZERO;
    }

    public LogSender(ObjectMapper objectMapper, AgentProperties properties) {
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.requestTimeout())
                .version(HttpClient.Version.HTTP_2)
                .build();
        this.endpoint = URI.create(properties.endpoint());
        this.requestTimeout = properties.requestTimeout();
    }

    public void send(String apiKey, List<LogPayload> payloads) {
        if (payloads.isEmpty()) {
            return;
        }
        try {
            String body = objectMapper.writeValueAsString(payloads);
            HttpRequest request = HttpRequest.newBuilder(endpoint)
                    .timeout(requestTimeout)
                    .header("Content-Type", "application/json")
                    .header("X-API-Key", apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn("As I Have Written returned HTTP {} for {} log(s): {}",
                        response.statusCode(), payloads.size(), trim(response.body()));
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        } catch (Exception ex) {
            log.warn("Failed to send {} log(s) to As I Have Written", payloads.size(), ex);
        }
    }

    private String trim(String value) {
        if (value == null) {
            return "";
        }
        return value.length() <= 512 ? value : value.substring(0, 512) + "...";
    }
}
