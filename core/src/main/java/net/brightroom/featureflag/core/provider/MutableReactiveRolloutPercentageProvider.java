package net.brightroom.featureflag.core.provider;

import java.util.Map;
import reactor.core.publisher.Mono;

/**
 * A reactive extension of {@link ReactiveRolloutPercentageProvider} that supports dynamic mutation
 * of rollout percentages at runtime.
 *
 * <p>Implementations must be thread-safe, as rollout percentages may be read and updated
 * concurrently.
 *
 * <p>This interface serves as an SPI for the actuator endpoint to update rollout percentages at
 * runtime without restarting the application.
 */
public interface MutableReactiveRolloutPercentageProvider
    extends ReactiveRolloutPercentageProvider {

  /**
   * Returns a snapshot of all currently configured rollout percentages.
   *
   * <p>The returned map must be an immutable copy; mutations to the returned map must not affect
   * the provider's internal state.
   *
   * @return a {@link Mono} emitting an immutable map of feature flag names to their rollout
   *     percentages
   */
  Mono<Map<String, Integer>> getRolloutPercentages();

  /**
   * Updates the rollout percentage for the specified feature flag.
   *
   * <p>If the feature flag does not have a configured rollout percentage, it is created.
   *
   * @param featureName the name of the feature flag to update
   * @param percentage the new rollout percentage (0–100)
   * @return a {@link Mono} that completes when the update is applied
   */
  Mono<Void> setRolloutPercentage(String featureName, int percentage);

  /**
   * Removes the rollout percentage for the specified feature flag.
   *
   * <p>If the feature flag does not have a configured rollout percentage, this method is a no-op
   * and emits {@code false}.
   *
   * @param featureName the name of the feature flag whose rollout percentage to remove
   * @return a {@link Mono} emitting {@code true} if the rollout percentage existed and was removed,
   *     {@code false} if it did not exist
   */
  Mono<Boolean> removeRolloutPercentage(String featureName);
}
