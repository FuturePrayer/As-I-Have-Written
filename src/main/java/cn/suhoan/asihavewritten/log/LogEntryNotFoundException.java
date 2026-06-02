package cn.suhoan.asihavewritten.log;

public class LogEntryNotFoundException extends RuntimeException {

    public LogEntryNotFoundException(String id) {
        super("Log entry not found: " + id);
    }
}
