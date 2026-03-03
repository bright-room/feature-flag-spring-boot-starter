package net.brightroom.featureflag.webflux.rollout;

import net.brightroom.featureflag.core.context.FeatureFlagContext;
import reactor.core.publisher.Mono;

/**
 * Reactive strategy for determining whether a given context is within the rollout bucket.
 *
 * <p>Implementations receive the feature name, context, and rollout percentage, and return a {@link
 * Mono} that emits {@code true} if the request/user should be included in the rollout. Register a
 * custom implementation as a {@code @Bean} to replace the default {@link
 * DefaultReactiveRolloutStrategy}.
 */
public interface ReactiveRolloutStrategy {

  /**
   * Determines whether the given context should be included in the rollout.
   *
   * @param featureName the feature flag name
   * @param context the context containing the user/request identifier
   * @param percentage the rollout percentage (0–100)
   * @return a {@link Mono} emitting {@code true} if the context is within the rollout bucket
   */
  Mono<Boolean> isInRollout(String featureName, FeatureFlagContext context, int percentage);
}
