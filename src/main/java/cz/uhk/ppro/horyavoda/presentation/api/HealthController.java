package cz.uhk.ppro.horyavoda.presentation.api;

import cz.uhk.ppro.horyavoda.application.service.HealthService;
import cz.uhk.ppro.horyavoda.application.service.HealthStatus;
import cz.uhk.ppro.horyavoda.presentation.dto.HealthResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class HealthController {

    private static final String SERVICE_NAME = "Hory a voda - Půjčovna";

    private final HealthService healthService;

    public HealthController(HealthService healthService) {
        this.healthService = healthService;
    }

    /** Healthcheck ověřující funkčnost aplikace i spojení s PostgreSQL (používá Docker Compose). */
    @GetMapping("/health")
    public ResponseEntity<HealthResponse> health() {
        HealthStatus status = healthService.check();
        if (status.databaseConnected()) {
            return ResponseEntity.ok(new HealthResponse("healthy", "connected", SERVICE_NAME));
        }
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new HealthResponse("unhealthy", "error: " + status.databaseError(), SERVICE_NAME));
    }
}
