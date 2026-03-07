package net.brightroom.featureflag.core.provider;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import reactor.core.publisher.Mono;

/**
 * A thread-safe, in-memory implementation of {@link MutableReactiveRolloutPercentageProvider}.
 *
 * <p>Rollout percentages are stored in a {@link ConcurrentHashMap}, allowing concurrent reads and
 * writes without external synchronization.
 */
public class MutableInMemoryReactiveRolloutPercentageProvider
    implements MutableReactiveRolloutPercentageProvider {

  private final ConcurrentHashMap<String, Integer> rolloutPercentages;

  /**
   * {@inheritDoc}
   *
   * <p>Returns an empty {@link Mono} for features not present in the rollout map.
   */
  @Override
  public Mono<Integer> getRolloutPercentage(String featureName) {
    Integer percentage = rolloutPercentages.get(featureName);
    return percentage != null ? Mono.just(percentage) : Mono.empty();
  }

  /** {@inheritDoc} */
  @Override
  public Mono<Map<String, Integer>> getRolloutPercentages() {
    return Mono.just(Map.copyOf(rolloutPercentages));
  }

  /** {@inheritDoc} */
  @Override
  public Mono<Void> setRolloutPercentage(String featureName, int percentage) {
    if (percentage < 0 || percentage > 100) {
      throw new IllegalArgumentException(
          "percentage must be between 0 and 100, but was: " + percentage);
    }
    return Mono.<Void>fromRunnable(() -> rolloutPercentages.put(featureName, percentage));
  }

  /** {@inheritDoc} */
  @Override
  public Mono<Boolean> removeRolloutPercentage(String featureName) {
    return Mono.fromCallable(() -> rolloutPercentages.remove(featureName) != null);
  }

  /**
   * Constructs a {@code MutableInMemoryReactiveRolloutPercentageProvider} with the given initial
   * rollout percentages.
   *
   * @param rolloutPercentages the initial rollout percentage map; copied defensively on
   *     construction
   */
  public MutableInMemoryReactiveRolloutPercentageProvider(Map<String, Integer> rolloutPercentages) {
    this.rolloutPercentages = new ConcurrentHashMap<>(rolloutPercentages);
  }
}
