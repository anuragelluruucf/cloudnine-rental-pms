package dev.cloudnine.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "cloudnine.ical.scheduling-enabled", havingValue = "true")
public class IcalSyncJob {

    private final IcalService icalService;

    public IcalSyncJob(IcalService icalService) {
        this.icalService = icalService;
    }

    @Scheduled(
            fixedDelayString = "${cloudnine.ical.sync-delay-ms:600000}",
            initialDelayString = "${cloudnine.ical.sync-initial-delay-ms:120000}"
    )
    public void sync() {
        icalService.syncAll();
    }
}
