package cn.suhoan.asihavewritten.log;

public record LogIngestCommand(
        LogSource source,
        IngestChannel channel,
        LogIngestRequest request) {
}
