package cn.suhoan.asihavewritten.log;

import org.springframework.stereotype.Component;

@Component
public class TestMqLogReceiver implements MqLogReceiver {

    private final MqLogMessageDecoder decoder;
    private final LogIngestService ingestService;
    private final LogSourceService sourceService;

    public TestMqLogReceiver(MqLogMessageDecoder decoder, LogIngestService ingestService, LogSourceService sourceService) {
        this.decoder = decoder;
        this.ingestService = ingestService;
        this.sourceService = sourceService;
    }

    @Override
    public void receive(byte[] payload) {
        LogIngestRequest request = decoder.decode(payload);
        LogSource source = sourceService.firstEnabledSource()
                .orElseThrow(() -> new IllegalStateException("No enabled API key configured"));
        ingestService.accept(new LogIngestCommand(source, IngestChannel.MQ_TEST, request));
    }
}
