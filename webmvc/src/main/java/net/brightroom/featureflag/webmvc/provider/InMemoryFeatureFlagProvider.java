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

  /**
   * Returns whether the specified feature is enabled.
   *
   * <p><b>Fail-open behavior:</b> If {@code featureName} is not present in the feature map, this
   * method returns {@code true} (enabled) by default. This means that a feature flag referenced in
   * a {@code @FeatureFlag} annotation but missing from the configuration will allow access rather
   * than blocking it. Ensure that all feature flag names used in {@code @FeatureFlag} annotations
   * are explicitly configured to avoid unintended access.
   *
   * @param featureName the name of the feature flag to check
   * @return {@code true} if the feature is enabled or not configured, {@code false} if explicitly
   *     disabled
   */
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
    this.features = Map.copyOf(features);
  }
}
