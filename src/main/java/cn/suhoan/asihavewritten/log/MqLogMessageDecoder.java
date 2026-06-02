package cn.suhoan.asihavewritten.log;

public interface MqLogMessageDecoder {

    LogIngestRequest decode(byte[] payload);
}
