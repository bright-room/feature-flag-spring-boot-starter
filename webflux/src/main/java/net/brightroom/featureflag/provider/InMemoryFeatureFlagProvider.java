package net.brightroom.featureflag.provider;

import java.util.Map;
import reactor.core.publisher.Mono;

/**
 * In-memory implementation of FeatureFlagProvider. Stores feature flags in memory using a map of
 * feature names to their enabled status.
 */
public class InMemoryFeatureFlagProvider implements FeatureFlagProvider {

  Map<String, Boolean> features;

  @Override
  public Mono<Boolean> isFeatureEnabled(String featureName) {
    return Mono.just(features.getOrDefault(featureName, true));
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
