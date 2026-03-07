package net.brightroom.featureflag.actuator.health;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
 * <p>When the provider implements {@link MutableFeatureFlagProvider}, flag information is retrieved
 * via {@link MutableFeatureFlagProvider#getFeatures()}. Otherwise, the configured feature names
 * from {@link FeatureFlagProperties} are probed individually via {@link
 * FeatureFlagProvider#isFeatureEnabled(String)}.
 *
 * <p>Additional details can be contributed by registering {@link HealthDetailsContributor} beans.
 */
public class FeatureFlagHealthIndicator extends AbstractHealthIndicator {

  private final FeatureFlagProvider provider;
  private final FeatureFlagProperties properties;
  private final List<HealthDetailsContributor> contributors;

  /**
   * Creates a new {@link FeatureFlagHealthIndicator}.
   *
   * @param provider the feature flag provider to check
   * @param properties the feature flag configuration properties
   */
  public FeatureFlagHealthIndicator(
      FeatureFlagProvider provider, FeatureFlagProperties properties) {
    this(provider, properties, List.of());
  }

  /**
   * Creates a new {@link FeatureFlagHealthIndicator} with custom detail contributors.
   *
   * @param provider the feature flag provider to check
   * @param properties the feature flag configuration properties
   * @param contributors the list of contributors that add custom details to the health response
   */
  public FeatureFlagHealthIndicator(
      FeatureFlagProvider provider,
      FeatureFlagProperties properties,
      List<HealthDetailsContributor> contributors) {
    super("Feature flag health check failed");
    this.provider = provider;
    this.properties = properties;
    this.contributors = contributors;
  }

  @Override
  protected void doHealthCheck(Health.Builder builder) {
    Map<String, Boolean> features;
    if (provider instanceof MutableFeatureFlagProvider mutableProvider) {
      features = mutableProvider.getFeatures();
    } else {
      var map = new LinkedHashMap<String, Boolean>();
      for (String name : properties.featureNames().keySet()) {
        map.put(name, provider.isFeatureEnabled(name));
      }
      features = map;
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

    for (HealthDetailsContributor contributor : contributors) {
      contributor.contributeDetails().forEach(builder::withDetail);
    }
  }
}
