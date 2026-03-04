package net.brightroom.featureflag.core.provider;

import java.util.Map;
import java.util.OptionalInt;

/**
 * An implementation of {@link RolloutPercentageProvider} that stores rollout percentages in memory
 * using a {@link Map}.
 *
 * <p>This class provides a simple, immutable in-memory mechanism to resolve rollout percentages.
 * When a feature has no configured rollout percentage, {@link OptionalInt#empty()} is returned.
 */
public class InMemoryRolloutPercentageProvider implements RolloutPercentageProvider {

  private final Map<String, Integer> rolloutPercentages;

  /**
   * {@inheritDoc}
   *
   * <p>Returns {@link OptionalInt#empty()} for features not present in the rollout map.
   */
  @Override
  public OptionalInt getRolloutPercentage(String featureName) {
    Integer percentage = rolloutPercentages.get(featureName);
    return percentage != null ? OptionalInt.of(percentage) : OptionalInt.empty();
  }

  /**
   * Constructs an instance with the provided rollout percentages.
   *
   * @param rolloutPercentages a map containing feature flag names as keys and their rollout
   *     percentages as values; copied defensively on construction
   */
  public InMemoryRolloutPercentageProvider(Map<String, Integer> rolloutPercentages) {
    this.rolloutPercentages = Map.copyOf(rolloutPercentages);
  }
}
