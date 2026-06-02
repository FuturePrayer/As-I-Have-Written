package cn.suhoan.asihavewritten.agent;

public enum DockerStream {
    STDOUT,
    STDERR;

    public LogLevel defaultLevel() {
        return this == STDERR ? LogLevel.ERROR : LogLevel.INFO;
    }
}
