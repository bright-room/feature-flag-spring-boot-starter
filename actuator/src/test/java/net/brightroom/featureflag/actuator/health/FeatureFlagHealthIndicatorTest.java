package net.brightroom.featureflag.actuator.health;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import net.brightroom.featureflag.core.properties.FeatureFlagProperties;
import net.brightroom.featureflag.core.provider.FeatureFlagProvider;
import net.brightroom.featureflag.core.provider.MutableInMemoryFeatureFlagProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.Status;

@ExtendWith(MockitoExtension.class)
class FeatureFlagHealthIndicatorTest {

  @Mock FeatureFlagProperties properties;

  @Test
  void health_isUp_withMutableProvider() {
    var provider =
        new MutableInMemoryFeatureFlagProvider(
            Map.of("feature-a", true, "feature-b", false), false);
    when(properties.defaultEnabled()).thenReturn(false);
    var indicator = new FeatureFlagHealthIndicator(provider, properties);

    Health health = indicator.health();

    assertThat(health.getStatus()).isEqualTo(Status.UP);
    assertThat(health.getDetails())
        .containsEntry("provider", "MutableInMemoryFeatureFlagProvider")
        .containsEntry("totalFlags", 2L)
        .containsEntry("enabledFlags", 1L)
        .containsEntry("disabledFlags", 1L)
        .containsEntry("defaultEnabled", false);
  }

  @Test
  void health_isUp_withNonMutableProvider() {
    FeatureFlagProvider provider = featureName -> "feature-a".equals(featureName);
    when(properties.featureNames()).thenReturn(Map.of("feature-a", true, "feature-b", false));
    when(properties.defaultEnabled()).thenReturn(false);
    var indicator = new FeatureFlagHealthIndicator(provider, properties);

    Health health = indicator.health();

    assertThat(health.getStatus()).isEqualTo(Status.UP);
    assertThat(health.getDetails())
        .containsEntry("totalFlags", 2L)
        .containsEntry("enabledFlags", 1L)
        .containsEntry("disabledFlags", 1L);
  }

  @Test
  void health_isDown_whenProviderThrowsException() {
    FeatureFlagProvider provider =
        featureName -> {
          throw new RuntimeException("Connection refused");
        };
    when(properties.featureNames()).thenReturn(Map.of("feature-a", true));
    var indicator = new FeatureFlagHealthIndicator(provider, properties);

    Health health = indicator.health();

    assertThat(health.getStatus()).isEqualTo(Status.DOWN);
    assertThat(health.getDetails()).containsKey("error");
  }

  @Test
  void health_isUp_withNoFlags() {
    var provider = new MutableInMemoryFeatureFlagProvider(Map.of(), false);
    when(properties.defaultEnabled()).thenReturn(false);
    var indicator = new FeatureFlagHealthIndicator(provider, properties);

    Health health = indicator.health();

    assertThat(health.getStatus()).isEqualTo(Status.UP);
    assertThat(health.getDetails())
        .containsEntry("totalFlags", 0L)
        .containsEntry("enabledFlags", 0L)
        .containsEntry("disabledFlags", 0L);
  }

  @Test
  void health_reflectsDefaultEnabled_true() {
    var provider = new MutableInMemoryFeatureFlagProvider(Map.of(), true);
    when(properties.defaultEnabled()).thenReturn(true);
    var indicator = new FeatureFlagHealthIndicator(provider, properties);

    Health health = indicator.health();

    assertThat(health.getDetails()).containsEntry("defaultEnabled", true);
  }

  @Test
  void health_includesProviderClassName() {
    var provider = new MutableInMemoryFeatureFlagProvider(Map.of(), false);
    when(properties.defaultEnabled()).thenReturn(false);
    var indicator = new FeatureFlagHealthIndicator(provider, properties);

    Health health = indicator.health();

    assertThat(health.getDetails()).containsEntry("provider", "MutableInMemoryFeatureFlagProvider");
  }

  @Test
  void health_includesContributorDetails() {
    var provider = new MutableInMemoryFeatureFlagProvider(Map.of(), false);
    when(properties.defaultEnabled()).thenReturn(false);
    HealthDetailsContributor contributor = () -> Map.of("connectionPoolSize", 10, "latencyMs", 5);
    var indicator = new FeatureFlagHealthIndicator(provider, properties, List.of(contributor));

    Health health = indicator.health();

    assertThat(health.getStatus()).isEqualTo(Status.UP);
    assertThat(health.getDetails())
        .containsEntry("connectionPoolSize", 10)
        .containsEntry("latencyMs", 5);
  }

  @Test
  void health_mergesMultipleContributorDetails() {
    var provider = new MutableInMemoryFeatureFlagProvider(Map.of(), false);
    when(properties.defaultEnabled()).thenReturn(false);
    HealthDetailsContributor contributor1 = () -> Map.of("key1", "value1");
    HealthDetailsContributor contributor2 = () -> Map.of("key2", "value2");
    var indicator =
        new FeatureFlagHealthIndicator(provider, properties, List.of(contributor1, contributor2));

    Health health = indicator.health();

    assertThat(health.getDetails()).containsEntry("key1", "value1").containsEntry("key2", "value2");
  }

  @Test
  void health_withNoContributors_hasDefaultDetails() {
    var provider = new MutableInMemoryFeatureFlagProvider(Map.of(), false);
    when(properties.defaultEnabled()).thenReturn(false);
    var indicator = new FeatureFlagHealthIndicator(provider, properties, List.of());

    Health health = indicator.health();

    assertThat(health.getStatus()).isEqualTo(Status.UP);
    assertThat(health.getDetails())
        .containsKeys("provider", "totalFlags", "enabledFlags", "disabledFlags", "defaultEnabled");
  }
}
