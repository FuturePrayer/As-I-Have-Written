package cn.suhoan.asihavewritten.log;

public interface MqLogReceiver {

    void receive(byte[] payload);
}
