package cz.uhk.ppro.horyavoda.infrastructure.time;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/** Systémové hodiny jako bean – testy je nahradí pevným časem (penále, překryv termínů). */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
