package net.brightroom.featureflag.core.provider;

import reactor.core.publisher.Mono;

/**
 * Reactive SPI for resolving the rollout percentage for a given feature flag.
 *
 * <p>Implementations provide the configured rollout percentage for each feature flag. When a
 * feature has no configured rollout percentage, an empty {@link Mono} is returned and the caller
 * falls back to the annotation's {@code rollout} attribute value.
 *
 * <p>Implement this interface and register it as a Spring bean to override the default in-memory
 * provider. For example, to read rollout percentages from a reactive data source.
 */
public interface ReactiveRolloutPercentageProvider {

  /**
   * Returns the configured rollout percentage for the specified feature.
   *
   * @param featureName the name of the feature flag
   * @return a {@link Mono} emitting the rollout percentage (0–100), or an empty {@link Mono} if no
   *     rollout percentage is configured for this feature
   */
  Mono<Integer> getRolloutPercentage(String featureName);
}
