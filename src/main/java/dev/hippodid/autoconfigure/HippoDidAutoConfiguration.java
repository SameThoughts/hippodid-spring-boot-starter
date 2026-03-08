package dev.hippodid.autoconfigure;

import dev.hippodid.client.HippoDidClient;
import dev.hippodid.health.HippoDidHealthIndicator;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Spring Boot auto-configuration for HippoDid.
 *
 * <p>Activated when {@code hippodid.api-key} is present in the application properties.
 * Creates the following beans:
 * <ul>
 *   <li>{@link HippoDidClient} — the main API client (always created when api-key is set)</li>
 *   <li>{@link HippoDidHealthIndicator} — Actuator health indicator (when actuator is on classpath)</li>
 * </ul>
 *
 * <p>To disable auto-configuration:
 * <pre>{@code
 * spring.autoconfigure.exclude=dev.hippodid.autoconfigure.HippoDidAutoConfiguration
 * }</pre>
 *
 * <p>To provide a custom client:
 * <pre>{@code
 * @Bean
 * public HippoDidClient hippoDidClient(HippoDidProperties props) {
 *     return new HippoDidClient(props, myCustomWebClient);
 * }
 * }</pre>
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "hippodid", name = "api-key")
@EnableConfigurationProperties(HippoDidProperties.class)
public class HippoDidAutoConfiguration {

    /**
     * Creates the {@link HippoDidClient} bean.
     *
     * <p>Uses a WebClient configured with:
     * <ul>
     *   <li>Base URL from {@code hippodid.base-url}</li>
     *   <li>{@code Authorization: Bearer <api-key>} header on every request</li>
     *   <li>Jackson with {@code JavaTimeModule} for {@code Instant} serialization</li>
     *   <li>10MB max response buffer (for large exports)</li>
     * </ul>
     */
    @Bean
    @ConditionalOnMissingBean
    public HippoDidClient hippoDidClient(HippoDidProperties properties) {
        return new HippoDidClient(properties);
    }

    /**
     * Creates the {@link HippoDidHealthIndicator} bean when Spring Actuator is present.
     *
     * <p>Exposes health at {@code GET /actuator/health/hippoDid}.
     * Override by defining your own {@code HealthIndicator} bean named {@code hippoDidHealthIndicator}.
     */
    @Bean
    @ConditionalOnClass(HealthIndicator.class)
    @ConditionalOnMissingBean(name = "hippoDidHealthIndicator")
    public HippoDidHealthIndicator hippoDidHealthIndicator(HippoDidClient client) {
        return new HippoDidHealthIndicator(client);
    }
}
