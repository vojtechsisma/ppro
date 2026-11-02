package cz.uhk.ppro.horyavoda.domain.model;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Penále za pozdní vrácení: za každý započatý den prodlení konkrétního kusu
 * se účtuje jeho sjednaná denní cena (snapshot z položky výpůjčky).
 * Vrácení o minutu později je tedy jeden započatý den, o 25 hodin později dva dny.
 */
public final class LateFeePolicy {

    private static final long SECONDS_PER_DAY = Duration.ofDays(1).toSeconds();

    private LateFeePolicy() {
    }

    /** Počet započatých dní prodlení; vrácení nejpozději v plánovaný konec je bez prodlení. */
    public static long startedDaysLate(LocalDateTime plannedEnd, LocalDateTime returnedAt) {
        if (!returnedAt.isAfter(plannedEnd)) {
            return 0;
        }
        long secondsLate = Duration.between(plannedEnd, returnedAt).toSeconds();
        return Math.max(1, (secondsLate + SECONDS_PER_DAY - 1) / SECONDS_PER_DAY);
    }

    public static BigDecimal lateFee(LocalDateTime plannedEnd, LocalDateTime returnedAt, BigDecimal unitDailyPrice) {
        return unitDailyPrice.multiply(BigDecimal.valueOf(startedDaysLate(plannedEnd, returnedAt)));
    }
}
