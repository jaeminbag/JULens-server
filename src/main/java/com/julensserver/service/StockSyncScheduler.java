package com.julensserver.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Profile("real")
@ConditionalOnProperty(
        name = "stock.sync.enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class StockSyncScheduler {

    private static final Logger log =
            LoggerFactory.getLogger(StockSyncScheduler.class);

    private final StockSyncService stockSyncService;
    private final boolean syncOnStartup;

    public StockSyncScheduler(
            StockSyncService stockSyncService,
            @Value("${stock.sync.on-startup:true}")
            boolean syncOnStartup
    ) {
        this.stockSyncService = stockSyncService;
        this.syncOnStartup = syncOnStartup;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void synchronizeOnStartup() {
        if (syncOnStartup) {
            synchronizeSafely("startup");
        }
    }

    @Scheduled(
            cron = "${stock.sync.schedule-cron:0 55 3 * * *}",
            zone = "America/New_York"
    )
    public void synchronizeDaily() {
        synchronizeSafely("daily");
    }

    private synchronized void synchronizeSafely(String trigger) {
        try {
            StockSyncResult result = stockSyncService.synchronize();
            log.info(
                    "Stock synchronization completed. trigger={}, "
                            + "received={}, activated={}, created={}, "
                            + "updated={}, deactivated={}",
                    trigger,
                    result.received(),
                    result.activated(),
                    result.created(),
                    result.updated(),
                    result.deactivated()
            );
        } catch (RuntimeException exception) {
            log.error(
                    "Stock synchronization failed. trigger={}",
                    trigger,
                    exception
            );
        }
    }
}
