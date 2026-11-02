package cz.uhk.ppro.horyavoda.presentation.api;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/** Hodiny pro testy, které lze přetočit (např. na 10:31, kdy propadá rezervace). */
class MutableClock extends Clock {

    private final ZoneId zone;
    private Instant instant;

    MutableClock(LocalDateTime start, ZoneId zone) {
        this.zone = zone;
        this.instant = start.atZone(zone).toInstant();
    }

    void setTo(LocalDateTime dateTime) {
        this.instant = dateTime.atZone(zone).toInstant();
    }

    @Override
    public ZoneId getZone() {
        return zone;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return Clock.fixed(instant, zone);
    }

    @Override
    public Instant instant() {
        return instant;
    }
}
