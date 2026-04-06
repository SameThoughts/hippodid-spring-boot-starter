package dev.hippodid.autoconfigure;

import dev.hippodid.client.HippoDidClient;
import dev.hippodid.health.HippoDidHealthIndicator;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link HippoDidAutoConfiguration}.
 */
class HippoDidAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(HippoDidAutoConfiguration.class));

    @Test
    void noBeansCreatedWhenApiKeyMissing() {
        // The env var HIPPODID_API_KEY may be set in dev environments (relaxed
        // binding maps it to hippodid.api-key), which would activate the
        // auto-config even though no property was explicitly provided. Override
        // with a system property so the context sees a blank value that still
        // won't satisfy @ConditionalOnProperty.
        String saved = System.getProperty("hippodid.api-key");
        try {
            System.clearProperty("hippodid.api-key");
            // Use a fresh runner without any hippodid properties. The env var
            // override trick: set the property to empty via withPropertyValues.
            // @ConditionalOnProperty with no havingValue matches any non-empty
            // value, so we need to override the env var binding entirely.
            new ApplicationContextRunner()
                    .withConfiguration(AutoConfigurations.of(HippoDidAutoConfiguration.class))
                    .withSystemProperties("hippodid.api-key=")
                    .run(ctx -> {
                        assertThat(ctx).doesNotHaveBean(HippoDidClient.class);
                        assertThat(ctx).doesNotHaveBean(HippoDidHealthIndicator.class);
                    });
        } finally {
            if (saved != null) {
                System.setProperty("hippodid.api-key", saved);
            }
        }
    }

    @Test
    void clientBeanCreatedWhenApiKeyPresent() {
        contextRunner
                .withPropertyValues("hippodid.api-key=hd_key_test")
                .run(ctx -> {
                    assertThat(ctx).hasSingleBean(HippoDidClient.class);
                });
    }

    @Test
    void healthIndicatorCreatedWhenActuatorPresent() {
        contextRunner
                .withPropertyValues("hippodid.api-key=hd_key_test")
                .run(ctx -> {
                    // HealthIndicator is on the test classpath via spring-boot-starter-actuator
                    assertThat(ctx).hasSingleBean(HippoDidHealthIndicator.class);
                });
    }

    @Test
    void propertiesDefaultBaseUrl() {
        contextRunner
                .withPropertyValues("hippodid.api-key=hd_key_test")
                .run(ctx -> {
                    HippoDidProperties props = ctx.getBean(HippoDidProperties.class);
                    assertThat(props.getBaseUrl()).isEqualTo("https://api.hippodid.com");
                    assertThat(props.getApiKey()).isEqualTo("hd_key_test");
                    assertThat(props.getCharacterId()).isNull();
                });
    }

    @Test
    void propertiesCustomBaseUrl() {
        contextRunner
                .withPropertyValues(
                        "hippodid.api-key=hd_key_test",
                        "hippodid.base-url=http://localhost:8080",
                        "hippodid.character-id=char-123")
                .run(ctx -> {
                    HippoDidProperties props = ctx.getBean(HippoDidProperties.class);
                    assertThat(props.getBaseUrl()).isEqualTo("http://localhost:8080");
                    assertThat(props.getCharacterId()).isEqualTo("char-123");
                });
    }

    @Test
    void customClientBeanTakesPrecedence() {
        contextRunner
                .withPropertyValues("hippodid.api-key=hd_key_test")
                .withBean(HippoDidClient.class, () -> {
                    HippoDidProperties props = new HippoDidProperties();
                    props.setApiKey("hd_key_custom");
                    props.setBaseUrl("http://custom.example.com");
                    return new HippoDidClient(props);
                })
                .run(ctx -> {
                    assertThat(ctx).hasSingleBean(HippoDidClient.class);
                    HippoDidClient client = ctx.getBean(HippoDidClient.class);
                    assertThat(client.baseUrl()).isEqualTo("http://custom.example.com");
                });
    }
}
