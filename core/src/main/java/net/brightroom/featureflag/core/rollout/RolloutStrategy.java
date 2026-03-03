package net.brightroom.featureflag.core.rollout;

import net.brightroom.featureflag.core.context.FeatureFlagContext;

/**
 * Strategy for determining whether a given context is within the rollout bucket.
 *
 * <p>Implementations receive the feature name, context, and rollout percentage, and return whether
 * the request/user should be included in the rollout. Register a custom implementation as a
 * {@code @Bean} to replace the default {@link DefaultRolloutStrategy}.
 */
public interface RolloutStrategy {

  /**
   * Determines whether the given context should be included in the rollout.
   *
   * @param featureName the feature flag name
   * @param context the context containing the user/request identifier
   * @param percentage the rollout percentage (0–100)
   * @return {@code true} if the context is within the rollout bucket
   */
  boolean isInRollout(String featureName, FeatureFlagContext context, int percentage);
}
