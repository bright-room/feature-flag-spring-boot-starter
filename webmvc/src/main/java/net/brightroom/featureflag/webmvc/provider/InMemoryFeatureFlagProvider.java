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

  Map<String, Boolean> features;

  @Override
  public boolean isFeatureEnabled(String featureName) {
    return features.getOrDefault(featureName, true);
  }

  /**
   * Constructs an instance of {@code InMemoryFeatureFlagProvider} with the provided feature flags.
   *
   * @param features a map containing feature flag names as keys and their activation status
   *     (enabled or disabled) as values. Feature names are represented as strings, and their
   *     corresponding statuses are represented as booleans, where {@code true} indicates that the
   *     feature is enabled, and {@code false} indicates that the feature is disabled.
   */
  public InMemoryFeatureFlagProvider(Map<String, Boolean> features) {
    this.features = features;
  }
}
