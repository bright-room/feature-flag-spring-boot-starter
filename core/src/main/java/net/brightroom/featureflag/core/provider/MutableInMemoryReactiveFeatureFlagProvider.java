package net.brightroom.featureflag.core.provider;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import reactor.core.publisher.Mono;

/**
 * A thread-safe, in-memory implementation of {@link MutableReactiveFeatureFlagProvider}.
 *
 * <p>Feature flags are stored in a {@link ConcurrentHashMap}, allowing concurrent reads and writes
 * without external synchronization. The fail-closed/fail-open policy is controlled by {@code
 * defaultEnabled}: when {@code false} (the default), unknown flags are treated as disabled.
 */
public class MutableInMemoryReactiveFeatureFlagProvider
    implements MutableReactiveFeatureFlagProvider {

  private final ConcurrentHashMap<String, Boolean> features;
  private final boolean defaultEnabled;

  /**
   * {@inheritDoc}
   *
   * <p>Returns the value of {@code defaultEnabled} for flags not present in the map.
   */
  @Override
  public Mono<Boolean> isFeatureEnabled(String featureName) {
    return Mono.just(features.getOrDefault(featureName, defaultEnabled));
  }

  /** {@inheritDoc} */
  @Override
  public Mono<Map<String, Boolean>> getFeatures() {
    return Mono.just(Map.copyOf(features));
  }

  /** {@inheritDoc} */
  @Override
  public Mono<Void> setFeatureEnabled(String featureName, boolean enabled) {
    features.put(featureName, enabled);
    return Mono.empty();
  }

  /** {@inheritDoc} */
  @Override
  public Mono<Void> removeFeature(String featureName) {
    features.remove(featureName);
    return Mono.empty();
  }

  /**
   * Constructs a {@code MutableInMemoryReactiveFeatureFlagProvider} with the given initial flags
   * and default enabled state.
   *
   * @param features the initial feature flag map; copied defensively on construction
   * @param defaultEnabled the fallback value for flags not present in the map
   */
  public MutableInMemoryReactiveFeatureFlagProvider(
      Map<String, Boolean> features, boolean defaultEnabled) {
    this.features = new ConcurrentHashMap<>(features);
    this.defaultEnabled = defaultEnabled;
  }
}
