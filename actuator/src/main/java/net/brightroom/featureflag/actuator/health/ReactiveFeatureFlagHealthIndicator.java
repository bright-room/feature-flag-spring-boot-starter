package net.brightroom.featureflag.actuator.health;

import java.util.Map;
import net.brightroom.featureflag.core.properties.FeatureFlagProperties;
import net.brightroom.featureflag.core.provider.MutableReactiveFeatureFlagProvider;
import net.brightroom.featureflag.core.provider.ReactiveFeatureFlagProvider;
import org.springframework.boot.health.contributor.AbstractReactiveHealthIndicator;
import org.springframework.boot.health.contributor.Health;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Reactive {@link org.springframework.boot.health.contributor.ReactiveHealthIndicator
 * ReactiveHealthIndicator} for the feature flag provider.
 *
 * <p>Reports {@link org.springframework.boot.health.contributor.Status#UP UP} when the provider
 * responds normally and flag information can be retrieved, and {@link
 * org.springframework.boot.health.contributor.Status#DOWN DOWN} when an exception occurs during the
 * health check.
 *
 * <p>Health details include:
 *
 * <ul>
 *   <li>{@code provider} — the simple class name of the provider implementation
 *   <li>{@code totalFlags} — total number of feature flags
 *   <li>{@code enabledFlags} — number of enabled flags
 *   <li>{@code disabledFlags} — number of disabled flags
 *   <li>{@code defaultEnabled} — the default-enabled policy from configuration
 * </ul>
 *
 * <p>When the provider implements {@link MutableReactiveFeatureFlagProvider}, flag information is
 * retrieved via {@link MutableReactiveFeatureFlagProvider#getFeatures()}. Otherwise, the configured
 * feature names from {@link FeatureFlagProperties} are probed individually via {@link
 * ReactiveFeatureFlagProvider#isFeatureEnabled(String)}.
 */
public class ReactiveFeatureFlagHealthIndicator extends AbstractReactiveHealthIndicator {

  private final ReactiveFeatureFlagProvider provider;
  private final FeatureFlagProperties properties;

  /**
   * Creates a new {@link ReactiveFeatureFlagHealthIndicator}.
   *
   * @param provider the reactive feature flag provider to check
   * @param properties the feature flag configuration properties
   */
  public ReactiveFeatureFlagHealthIndicator(
      ReactiveFeatureFlagProvider provider, FeatureFlagProperties properties) {
    super("Feature flag health check failed");
    this.provider = provider;
    this.properties = properties;
  }

  @Override
  protected Mono<Health> doHealthCheck(Health.Builder builder) {
    Mono<Map<String, Boolean>> featuresMono;
    if (provider instanceof MutableReactiveFeatureFlagProvider mutableProvider) {
      featuresMono = mutableProvider.getFeatures();
    } else {
      featuresMono =
          Flux.fromIterable(properties.featureNames().keySet())
              .flatMap(
                  name -> provider.isFeatureEnabled(name).map(enabled -> Map.entry(name, enabled)))
              .collectMap(Map.Entry::getKey, Map.Entry::getValue);
    }

    return featuresMono.map(
        features -> {
          long totalCount = features.size();
          long enabledCount = features.values().stream().filter(Boolean::booleanValue).count();
          long disabledCount = totalCount - enabledCount;

          return builder
              .up()
              .withDetail("provider", provider.getClass().getSimpleName())
              .withDetail("totalFlags", totalCount)
              .withDetail("enabledFlags", enabledCount)
              .withDetail("disabledFlags", disabledCount)
              .withDetail("defaultEnabled", properties.defaultEnabled())
              .build();
        });
  }
}
