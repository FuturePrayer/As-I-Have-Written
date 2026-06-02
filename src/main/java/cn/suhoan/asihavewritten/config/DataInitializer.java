package cn.suhoan.asihavewritten.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import cn.suhoan.asihavewritten.log.LogSourceService;

@Component
public class DataInitializer implements ApplicationRunner {

    private final LogSourceService logSourceService;

    public DataInitializer(LogSourceService logSourceService) {
        this.logSourceService = logSourceService;
    }

    @Override
    public void run(ApplicationArguments args) {
        logSourceService.ensureDefaultSource();
    }
}
