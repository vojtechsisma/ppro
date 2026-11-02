package cz.uhk.ppro.horyavoda.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Penále za každý započatý den prodlení konkrétního kusu. */
class LateFeePolicyTest {

    private static final LocalDateTime PLANNED_END = LocalDateTime.of(2027, 1, 24, 18, 0);
    private static final BigDecimal DAILY_PRICE = new BigDecimal("250.00");

    @Test
    void returnOnTimeOrEarlyHasNoFee() {
        assertEquals(0, LateFeePolicy.startedDaysLate(PLANNED_END, PLANNED_END));
        assertEquals(0, LateFeePolicy.startedDaysLate(PLANNED_END, PLANNED_END.minusHours(3)));
    }

    @Test
    void oneMinuteLateIsOneStartedDay() {
        assertEquals(1, LateFeePolicy.startedDaysLate(PLANNED_END, PLANNED_END.plusMinutes(1)));
    }

    @Test
    void exactlyOneDayLateIsStillOneDay() {
        assertEquals(1, LateFeePolicy.startedDaysLate(PLANNED_END, PLANNED_END.plusDays(1)));
    }

    @Test
    void moreThanOneDayStartsTheSecondDay() {
        assertEquals(2, LateFeePolicy.startedDaysLate(PLANNED_END, PLANNED_END.plusDays(1).plusSeconds(1)));
    }

    @Test
    void feeIsDailyPriceTimesStartedDays() {
        BigDecimal fee = LateFeePolicy.lateFee(PLANNED_END, PLANNED_END.plusHours(25), DAILY_PRICE);
        assertEquals(0, new BigDecimal("500.00").compareTo(fee));
    }
}
