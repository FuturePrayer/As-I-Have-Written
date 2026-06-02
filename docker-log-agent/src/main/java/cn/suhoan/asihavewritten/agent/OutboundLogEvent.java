package cn.suhoan.asihavewritten.agent;

public record OutboundLogEvent(String apiKey, LogPayload payload) {
}
