package net.brightroom.featureflag.core.provider;

import java.util.OptionalInt;

/**
 * SPI for resolving the rollout percentage for a given feature flag.
 *
 * <p>Implementations provide the configured rollout percentage for each feature flag. When a
 * feature has no configured rollout percentage, {@link OptionalInt#empty()} is returned and the
 * caller falls back to the annotation's {@code rollout} attribute value.
 *
 * <p>Implement this interface and register it as a Spring bean to override the default in-memory
 * provider. For example, to read rollout percentages from a database or remote config service.
 */
public interface RolloutPercentageProvider {

  /**
   * Returns the configured rollout percentage for the specified feature.
   *
   * @param featureName the name of the feature flag
   * @return an {@link OptionalInt} containing the rollout percentage (0–100), or {@link
   *     OptionalInt#empty()} if no rollout percentage is configured for this feature
   */
  OptionalInt getRolloutPercentage(String featureName);
}
