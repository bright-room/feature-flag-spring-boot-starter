package net.brightroom.featureflag.webmvc.provider;

import java.util.Map;

/**
 * An implementation of {@link FeatureFlagProvider} that stores feature flag configurations in
 * memory using a {@link Map}.
 *
 * <p>This class provides a simple mechanism to check whether specific features are enabled or
 * disabled based on an in-memory map of feature flag names and their corresponding status. It
 * allows flexibility for testing or applications that require basic, non-persistent feature flag
 * management.
 */
public class InMemoryFeatureFlagProvider implements FeatureFlagProvider {

  private final Map<String, Boolean> features;
  private final boolean defaultEnabled;

  /**
   * Returns whether the specified feature is enabled.
   *
   * <p><b>Fail-closed behavior (default):</b> If {@code featureName} is not present in the feature
   * map, this method returns the value of {@code defaultEnabled} (defaults to {@code false}). With
   * the default configuration, a feature flag referenced in a {@code @FeatureFlag} annotation but
   * missing from the configuration will block access. Set {@code feature-flags.default-enabled:
   * true} in your configuration for fail-open behavior.
   *
   * @param featureName the name of the feature flag to check
   * @return {@code true} if the feature is explicitly enabled, {@code false} if explicitly disabled
   *     or not configured (with default fail-closed behavior)
   */
  @Override
  public boolean isFeatureEnabled(String featureName) {
    return features.getOrDefault(featureName, defaultEnabled);
  }

  /**
   * Constructs an instance of {@code InMemoryFeatureFlagProvider} with the provided feature flags
   * and default enabled status.
   *
   * @param features a map containing feature flag names as keys and their activation status
   *     (enabled or disabled) as values. Feature names are represented as strings, and their
   *     corresponding statuses are represented as booleans, where {@code true} indicates that the
   *     feature is enabled, and {@code false} indicates that the feature is disabled.
   * @param defaultEnabled the default enabled status for features not present in the map. Use
   *     {@code false} for fail-closed behavior (blocks access for undefined flags) or {@code true}
   *     for fail-open behavior (allows access for undefined flags).
   */
  public InMemoryFeatureFlagProvider(Map<String, Boolean> features, boolean defaultEnabled) {
    this.features = Map.copyOf(features);
    this.defaultEnabled = defaultEnabled;
  }
}
