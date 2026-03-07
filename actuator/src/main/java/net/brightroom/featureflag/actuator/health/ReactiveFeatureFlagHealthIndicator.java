package net.brightroom.featureflag.actuator.health;

import java.time.Duration;
import java.util.List;
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
 * health check or when the provider does not respond within the configured timeout.
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
 *
 * <p>When a timeout is configured via {@link FeatureFlagHealthProperties#timeout()}, the health
 * check will report {@code DOWN} if the provider does not respond within that duration.
 *
 * <p>Additional details can be contributed by registering {@link ReactiveHealthDetailsContributor}
 * beans.
 */
public class ReactiveFeatureFlagHealthIndicator extends AbstractReactiveHealthIndicator {

  private final ReactiveFeatureFlagProvider provider;
  private final FeatureFlagProperties properties;
  private final Duration timeout;
  private final List<ReactiveHealthDetailsContributor> contributors;

  /**
   * Creates a new {@link ReactiveFeatureFlagHealthIndicator}.
   *
   * @param provider the reactive feature flag provider to check
   * @param properties the feature flag configuration properties
   */
  public ReactiveFeatureFlagHealthIndicator(
      ReactiveFeatureFlagProvider provider, FeatureFlagProperties properties) {
    this(provider, properties, null, List.of());
  }

  /**
   * Creates a new {@link ReactiveFeatureFlagHealthIndicator} with timeout and custom detail
   * contributors.
   *
   * @param provider the reactive feature flag provider to check
   * @param properties the feature flag configuration properties
   * @param timeout the maximum time to wait for the provider, or {@code null} for no timeout
   * @param contributors the list of contributors that add custom details to the health response
   */
  public ReactiveFeatureFlagHealthIndicator(
      ReactiveFeatureFlagProvider provider,
      FeatureFlagProperties properties,
      Duration timeout,
      List<ReactiveHealthDetailsContributor> contributors) {
    super("Feature flag health check failed");
    this.provider = provider;
    this.properties = properties;
    this.timeout = timeout;
    this.contributors = contributors;
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

    if (timeout != null) {
      featuresMono = featuresMono.timeout(timeout);
    }

    return featuresMono.flatMap(
        features -> {
          long totalCount = features.size();
          long enabledCount = features.values().stream().filter(Boolean::booleanValue).count();
          long disabledCount = totalCount - enabledCount;

          builder
              .up()
              .withDetail("provider", provider.getClass().getSimpleName())
              .withDetail("totalFlags", totalCount)
              .withDetail("enabledFlags", enabledCount)
              .withDetail("disabledFlags", disabledCount)
              .withDetail("defaultEnabled", properties.defaultEnabled());

          return Flux.fromIterable(contributors)
              .flatMap(ReactiveHealthDetailsContributor::contributeDetails)
              .doOnNext(details -> details.forEach(builder::withDetail))
              .then(Mono.fromCallable(builder::build));
        });
  }
}
