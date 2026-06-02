package cn.suhoan.asihavewritten.config;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import cn.suhoan.asihavewritten.log.LogSourceService;

@Component
public class ServiceCleanupScheduler {

    private final LogSourceService logSourceService;

    public ServiceCleanupScheduler(LogSourceService logSourceService) {
        this.logSourceService = logSourceService;
    }

    @Scheduled(fixedDelayString = "${aih.service-cleanup.fixed-delay:10m}", initialDelayString = "${aih.service-cleanup.initial-delay:10m}")
    public void cleanupUnusedServices() {
        logSourceService.cleanupUnusedServices();
    }
}
