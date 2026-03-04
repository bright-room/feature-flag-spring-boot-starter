package net.brightroom.featureflag.core.provider;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A thread-safe, in-memory implementation of {@link MutableFeatureFlagProvider}.
 *
 * <p>Feature flags are stored in a {@link ConcurrentHashMap}, allowing concurrent reads and writes
 * without external synchronization. The fail-closed/fail-open policy is controlled by {@code
 * defaultEnabled}: when {@code false} (the default), unknown flags are treated as disabled.
 */
public class MutableInMemoryFeatureFlagProvider implements MutableFeatureFlagProvider {

  private final ConcurrentHashMap<String, Boolean> features;
  private final boolean defaultEnabled;

  /**
   * {@inheritDoc}
   *
   * <p>Returns the value of {@code defaultEnabled} for flags not present in the map.
   */
  @Override
  public boolean isFeatureEnabled(String featureName) {
    return features.getOrDefault(featureName, defaultEnabled);
  }

  /** {@inheritDoc} */
  @Override
  public Map<String, Boolean> getFeatures() {
    return Map.copyOf(features);
  }

  /** {@inheritDoc} */
  @Override
  public void setFeatureEnabled(String featureName, boolean enabled) {
    features.put(featureName, enabled);
  }

  /** {@inheritDoc} */
  @Override
  public void removeFeature(String featureName) {
    features.remove(featureName);
  }

  /**
   * Constructs a {@code MutableInMemoryFeatureFlagProvider} with the given initial flags and
   * default enabled state.
   *
   * @param features the initial feature flag map; copied defensively on construction
   * @param defaultEnabled the fallback value for flags not present in the map
   */
  public MutableInMemoryFeatureFlagProvider(Map<String, Boolean> features, boolean defaultEnabled) {
    this.features = new ConcurrentHashMap<>(features);
    this.defaultEnabled = defaultEnabled;
  }
}
