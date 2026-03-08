package dev.hippodid.health;

import dev.hippodid.client.HippoDidClient;
import dev.hippodid.client.HippoDidException;
import dev.hippodid.client.model.TierInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link HippoDidHealthIndicator}.
 */
@ExtendWith(MockitoExtension.class)
class HippoDidHealthIndicatorTest {

    @Mock
    private HippoDidClient client;

    @Test
    void statusUpWhenTierCallSucceeds() {
        TierInfo tier = new TierInfo(
                "DEVELOPER", 30, 5, 3, 3, 15, true, true, true, true);
        when(client.tier()).thenReturn(tier);
        when(client.baseUrl()).thenReturn("https://api.hippodid.com");

        HippoDidHealthIndicator indicator = new HippoDidHealthIndicator(client);
        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsEntry("tier", "DEVELOPER");
        assertThat(health.getDetails()).containsEntry("characters", "5/30");
        assertThat(health.getDetails()).containsEntry("aiExtraction", true);
        assertThat(health.getDetails()).containsEntry("baseUrl", "https://api.hippodid.com");
    }

    @Test
    void statusDownWhenTierCallThrowsHippoDidException() {
        when(client.tier()).thenThrow(new HippoDidException(401, "Unauthorized", "Invalid API key"));

        HippoDidHealthIndicator indicator = new HippoDidHealthIndicator(client);
        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails()).containsKey("error");
        assertThat(health.getDetails()).containsEntry("statusCode", 401);
    }

    @Test
    void statusDownWhenUnexpectedExceptionThrown() {
        when(client.tier()).thenThrow(new RuntimeException("Connection refused"));

        HippoDidHealthIndicator indicator = new HippoDidHealthIndicator(client);
        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails()).containsKey("error");
    }

    @Test
    void teamSharingDetailReflectsTierFeature() {
        TierInfo freeTier = new TierInfo(
                "FREE", 3, 1, 1, 1, 60, false, false, false, false);
        when(client.tier()).thenReturn(freeTier);
        when(client.baseUrl()).thenReturn("https://api.hippodid.com");

        Health health = new HippoDidHealthIndicator(client).health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsEntry("tier", "FREE");
        assertThat(health.getDetails()).containsEntry("teamSharing", false);
    }
}
