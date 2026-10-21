package cz.uhk.ppro.horyavoda.application.service;

import cz.uhk.ppro.horyavoda.infrastructure.persistence.DatabaseHealthProbe;
import org.springframework.stereotype.Service;

/** Zjištění provozního stavu aplikace a jejího spojení s databází. */
@Service
public class HealthService {

    private final DatabaseHealthProbe databaseHealthProbe;

    public HealthService(DatabaseHealthProbe databaseHealthProbe) {
        this.databaseHealthProbe = databaseHealthProbe;
    }

    public HealthStatus check() {
        try {
            databaseHealthProbe.ping();
            return new HealthStatus(true, null);
        } catch (RuntimeException exception) {
            return new HealthStatus(false, exception.getMessage());
        }
    }
}
