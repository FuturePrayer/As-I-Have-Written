package cn.suhoan.asihavewritten.agent;

public enum LogLevel {
    TRACE,
    DEBUG,
    INFO,
    WARN,
    ERROR;

    public static LogLevel from(String value, LogLevel fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return LogLevel.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return fallback;
        }
    }
}
