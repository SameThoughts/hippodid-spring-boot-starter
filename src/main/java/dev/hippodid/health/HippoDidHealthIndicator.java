package dev.hippodid.health;

import dev.hippodid.client.HippoDidClient;
import dev.hippodid.client.HippoDidException;
import dev.hippodid.client.model.TierInfo;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;

/**
 * Spring Boot Actuator health indicator for the HippoDid API connection.
 *
 * <p>Registered automatically when {@code spring-boot-starter-actuator} is on the
 * classpath and {@code hippodid.api-key} is configured. Visible at:
 * {@code GET /actuator/health/hippoDid}
 *
 * <h3>Healthy response example</h3>
 * <pre>{@code
 * {
 *   "status": "UP",
 *   "components": {
 *     "hippoDid": {
 *       "status": "UP",
 *       "details": {
 *         "tier": "DEVELOPER",
 *         "characters": "2/30",
 *         "aiExtraction": true,
 *         "baseUrl": "https://api.hippodid.com"
 *       }
 *     }
 *   }
 * }
 * }</pre>
 *
 * <h3>Unhealthy response example</h3>
 * <pre>{@code
 * {
 *   "status": "DOWN",
 *   "components": {
 *     "hippoDid": {
 *       "status": "DOWN",
 *       "details": {
 *         "error": "[401] Unauthorized: Invalid API key"
 *       }
 *     }
 *   }
 * }
 * }</pre>
 */
public class HippoDidHealthIndicator implements HealthIndicator {

    private final HippoDidClient client;

    public HippoDidHealthIndicator(HippoDidClient client) {
        this.client = client;
    }

    @Override
    public Health health() {
        try {
            TierInfo tier = client.tier();
            return Health.up()
                    .withDetail("tier", tier.tier())
                    .withDetail("characters", tier.currentCharacterCount() + "/" + tier.maxCharacters())
                    .withDetail("aiExtraction", tier.aiExtractionAvailable())
                    .withDetail("teamSharing", tier.teamSharingEnabled())
                    .withDetail("baseUrl", client.baseUrl())
                    .build();
        } catch (HippoDidException e) {
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .withDetail("statusCode", e.statusCode())
                    .build();
        } catch (Exception e) {
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}
