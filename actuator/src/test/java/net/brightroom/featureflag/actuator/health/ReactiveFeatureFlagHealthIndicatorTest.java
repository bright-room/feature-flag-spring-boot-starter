package net.brightroom.featureflag.actuator.health;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.Map;
import net.brightroom.featureflag.core.properties.FeatureFlagProperties;
import net.brightroom.featureflag.core.provider.MutableInMemoryReactiveFeatureFlagProvider;
import net.brightroom.featureflag.core.provider.ReactiveFeatureFlagProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.Status;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class ReactiveFeatureFlagHealthIndicatorTest {

  @Mock FeatureFlagProperties properties;

  @Test
  void health_isUp_withMutableReactiveProvider() {
    var provider =
        new MutableInMemoryReactiveFeatureFlagProvider(
            Map.of("feature-a", true, "feature-b", false), false);
    when(properties.defaultEnabled()).thenReturn(false);
    var indicator = new ReactiveFeatureFlagHealthIndicator(provider, properties, null);

    Health health = indicator.health().block();

    assertThat(health.getStatus()).isEqualTo(Status.UP);
    assertThat(health.getDetails())
        .containsEntry("provider", "MutableInMemoryReactiveFeatureFlagProvider")
        .containsEntry("totalFlags", 2L)
        .containsEntry("enabledFlags", 1L)
        .containsEntry("disabledFlags", 1L)
        .containsEntry("defaultEnabled", false);
  }

  @Test
  void health_isUp_withNonMutableReactiveProvider() {
    ReactiveFeatureFlagProvider provider =
        featureName -> Mono.just("feature-a".equals(featureName));
    when(properties.featureNames()).thenReturn(Map.of("feature-a", true, "feature-b", false));
    when(properties.defaultEnabled()).thenReturn(false);
    var indicator = new ReactiveFeatureFlagHealthIndicator(provider, properties, null);

    Health health = indicator.health().block();

    assertThat(health.getStatus()).isEqualTo(Status.UP);
    assertThat(health.getDetails())
        .containsEntry("totalFlags", 2L)
        .containsEntry("enabledFlags", 1L)
        .containsEntry("disabledFlags", 1L);
  }

  @Test
  void health_isDown_whenProviderThrowsException() {
    ReactiveFeatureFlagProvider provider =
        featureName -> Mono.error(new RuntimeException("Connection refused"));
    when(properties.featureNames()).thenReturn(Map.of("feature-a", true));
    var indicator = new ReactiveFeatureFlagHealthIndicator(provider, properties, null);

    Health health = indicator.health().block();

    assertThat(health.getStatus()).isEqualTo(Status.DOWN);
    assertThat(health.getDetails()).containsKey("error");
  }

  @Test
  void health_isUp_withNoFlags() {
    var provider = new MutableInMemoryReactiveFeatureFlagProvider(Map.of(), false);
    when(properties.defaultEnabled()).thenReturn(false);
    var indicator = new ReactiveFeatureFlagHealthIndicator(provider, properties, null);

    Health health = indicator.health().block();

    assertThat(health.getStatus()).isEqualTo(Status.UP);
    assertThat(health.getDetails())
        .containsEntry("totalFlags", 0L)
        .containsEntry("enabledFlags", 0L)
        .containsEntry("disabledFlags", 0L);
  }

  @Test
  void health_reflectsDefaultEnabled_true() {
    var provider = new MutableInMemoryReactiveFeatureFlagProvider(Map.of(), true);
    when(properties.defaultEnabled()).thenReturn(true);
    var indicator = new ReactiveFeatureFlagHealthIndicator(provider, properties, null);

    Health health = indicator.health().block();

    assertThat(health.getDetails()).containsEntry("defaultEnabled", true);
  }

  @Test
  void health_includesProviderClassName() {
    var provider = new MutableInMemoryReactiveFeatureFlagProvider(Map.of(), false);
    when(properties.defaultEnabled()).thenReturn(false);
    var indicator = new ReactiveFeatureFlagHealthIndicator(provider, properties, null);

    Health health = indicator.health().block();

    assertThat(health.getDetails())
        .containsEntry("provider", "MutableInMemoryReactiveFeatureFlagProvider");
  }

  @Test
  void health_isDown_whenProviderExceedsTimeout() {
    ReactiveFeatureFlagProvider provider =
        featureName -> Mono.just(true).delayElement(Duration.ofSeconds(5));
    when(properties.featureNames()).thenReturn(Map.of("feature-a", true));
    var indicator =
        new ReactiveFeatureFlagHealthIndicator(provider, properties, Duration.ofMillis(100));

    Health health = indicator.health().block();

    assertThat(health.getStatus()).isEqualTo(Status.DOWN);
    assertThat(health.getDetails()).containsKey("error");
  }

  @Test
  void health_isUp_whenProviderRespondsWithinTimeout() {
    var provider = new MutableInMemoryReactiveFeatureFlagProvider(Map.of("feature-a", true), false);
    when(properties.defaultEnabled()).thenReturn(false);
    var indicator =
        new ReactiveFeatureFlagHealthIndicator(provider, properties, Duration.ofSeconds(5));

    Health health = indicator.health().block();

    assertThat(health.getStatus()).isEqualTo(Status.UP);
  }
}
