package net.brightroom.featureflag.provider;

import java.util.Map;

/**
 * In-memory implementation of FeatureFlagProvider. Stores feature flags in memory using a map of
 * feature names to their enabled status.
 */
public class InMemoryFeatureFlagProvider implements FeatureFlagProvider {

  private final Map<String, Boolean> features;

  @Override
  public boolean isFeatureEnabled(String featureName) {
    return features.getOrDefault(featureName, true);
  }

  /**
   * Constractor
   *
   * @param features property
   */
  public InMemoryFeatureFlagProvider(Map<String, Boolean> features) {
    this.features = features;
  }
}
