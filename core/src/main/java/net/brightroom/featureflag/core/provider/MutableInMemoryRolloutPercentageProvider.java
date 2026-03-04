package net.brightroom.featureflag.core.provider;

import java.util.Map;
import java.util.OptionalInt;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A thread-safe, in-memory implementation of {@link MutableRolloutPercentageProvider}.
 *
 * <p>Rollout percentages are stored in a {@link ConcurrentHashMap}, allowing concurrent reads and
 * writes without external synchronization.
 */
public class MutableInMemoryRolloutPercentageProvider implements MutableRolloutPercentageProvider {

  private final ConcurrentHashMap<String, Integer> rolloutPercentages;

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

  /** {@inheritDoc} */
  @Override
  public Map<String, Integer> getRolloutPercentages() {
    return Map.copyOf(rolloutPercentages);
  }

  /** {@inheritDoc} */
  @Override
  public void setRolloutPercentage(String featureName, int percentage) {
    rolloutPercentages.put(featureName, percentage);
  }

  /**
   * Constructs a {@code MutableInMemoryRolloutPercentageProvider} with the given initial rollout
   * percentages.
   *
   * @param rolloutPercentages the initial rollout percentage map; copied defensively on
   *     construction
   */
  public MutableInMemoryRolloutPercentageProvider(Map<String, Integer> rolloutPercentages) {
    this.rolloutPercentages = new ConcurrentHashMap<>(rolloutPercentages);
  }
}
