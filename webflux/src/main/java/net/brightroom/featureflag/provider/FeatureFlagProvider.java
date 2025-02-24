package net.brightroom.featureflag.provider;

import reactor.core.publisher.Mono;

/**
 * Interface for feature flag providers. Implementations of this interface are responsible for
 * determining whether specific features are enabled or disabled.
 */
public interface FeatureFlagProvider {

  /**
   * Checks if a specific feature is enabled.
   *
   * @param featureName the name of the feature to check
   * @return true if the feature is enabled, false otherwise
   */
  Mono<Boolean> isFeatureEnabled(String featureName);
}
