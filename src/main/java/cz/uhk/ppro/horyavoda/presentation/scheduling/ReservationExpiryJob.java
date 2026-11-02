package cz.uhk.ppro.horyavoda.presentation.scheduling;

import cz.uhk.ppro.horyavoda.application.service.ReservationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Každou minutu označí rezervace, které si nikdo nevyzvedl do 10:00 + 30 minut, jako propadlé.
 * Je to vstupní bod do aplikace stejně jako controller, proto leží v prezentační vrstvě.
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "app.reservations.expiry-job-enabled", havingValue = "true", matchIfMissing = true)
public class ReservationExpiryJob {

    private static final Logger log = LoggerFactory.getLogger(ReservationExpiryJob.class);

    private final ReservationService reservationService;

    public ReservationExpiryJob(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @Scheduled(fixedDelayString = "PT1M", initialDelayString = "PT10S")
    public void expireReservations() {
        int expired = reservationService.expireOverdueReservations();
        if (expired > 0) {
            log.info("Propadlo {} rezervací (nevyzvednuto včas).", expired);
        }
    }
}
