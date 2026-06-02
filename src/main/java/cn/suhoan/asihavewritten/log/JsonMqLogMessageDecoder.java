package cn.suhoan.asihavewritten.log;

import org.springframework.stereotype.Component;

import tools.jackson.databind.ObjectMapper;

@Component
public class JsonMqLogMessageDecoder implements MqLogMessageDecoder {

    private final ObjectMapper objectMapper;

    public JsonMqLogMessageDecoder(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public LogIngestRequest decode(byte[] payload) {
        try {
            return objectMapper.readValue(payload, LogIngestRequest.class);
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException("Invalid MQ log message", ex);
        }
    }
}
