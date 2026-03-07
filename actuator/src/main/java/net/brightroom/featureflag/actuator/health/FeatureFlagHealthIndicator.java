package net.brightroom.featureflag.actuator.health;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import net.brightroom.featureflag.core.properties.FeatureFlagProperties;
import net.brightroom.featureflag.core.provider.FeatureFlagProvider;
import net.brightroom.featureflag.core.provider.MutableFeatureFlagProvider;
import org.springframework.boot.health.contributor.AbstractHealthIndicator;
import org.springframework.boot.health.contributor.Health;

/**
 * {@link org.springframework.boot.health.contributor.HealthIndicator HealthIndicator} for the
 * feature flag provider.
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
 * <p>When the provider implements {@link MutableFeatureFlagProvider}, flag information is retrieved
 * via {@link MutableFeatureFlagProvider#getFeatures()}. Otherwise, the configured feature names
 * from {@link FeatureFlagProperties} are probed individually via {@link
 * FeatureFlagProvider#isFeatureEnabled(String)}.
 *
 * <p>When a timeout is configured via {@link FeatureFlagHealthProperties#timeout()}, the health
 * check will report {@code DOWN} if the provider does not respond within that duration.
 */
public class FeatureFlagHealthIndicator extends AbstractHealthIndicator {

  private final FeatureFlagProvider provider;
  private final FeatureFlagProperties properties;
  private final Duration timeout;

  /**
   * Creates a new {@link FeatureFlagHealthIndicator}.
   *
   * @param provider the feature flag provider to check
   * @param properties the feature flag configuration properties
   * @param timeout the maximum time to wait for the provider, or {@code null} for no timeout
   */
  public FeatureFlagHealthIndicator(
      FeatureFlagProvider provider, FeatureFlagProperties properties, Duration timeout) {
    super("Feature flag health check failed");
    this.provider = provider;
    this.properties = properties;
    this.timeout = timeout;
  }

  @Override
  protected void doHealthCheck(Health.Builder builder) throws Exception {
    Map<String, Boolean> features;
    if (timeout != null) {
      features =
          CompletableFuture.supplyAsync(this::fetchFeatures)
              .get(timeout.toMillis(), TimeUnit.MILLISECONDS);
    } else {
      features = fetchFeatures();
    }

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
  }

  private Map<String, Boolean> fetchFeatures() {
    if (provider instanceof MutableFeatureFlagProvider mutableProvider) {
      return mutableProvider.getFeatures();
    }
    var map = new LinkedHashMap<String, Boolean>();
    for (String name : properties.featureNames().keySet()) {
      map.put(name, provider.isFeatureEnabled(name));
    }
    return map;
  }
}
