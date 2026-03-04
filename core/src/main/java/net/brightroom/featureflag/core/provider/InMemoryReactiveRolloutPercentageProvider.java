package net.brightroom.featureflag.core.provider;

import java.util.Map;
import reactor.core.publisher.Mono;

/**
 * An implementation of {@link ReactiveRolloutPercentageProvider} that stores rollout percentages in
 * memory using a {@link Map}.
 *
 * <p>This class provides a simple, immutable in-memory mechanism to resolve rollout percentages
 * reactively. When a feature has no configured rollout percentage, an empty {@link Mono} is
 * returned.
 */
public class InMemoryReactiveRolloutPercentageProvider
    implements ReactiveRolloutPercentageProvider {

  private final Map<String, Integer> rolloutPercentages;

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

  /**
   * Constructs an instance with the provided rollout percentages.
   *
   * @param rolloutPercentages a map containing feature flag names as keys and their rollout
   *     percentages as values; copied defensively on construction
   */
  public InMemoryReactiveRolloutPercentageProvider(Map<String, Integer> rolloutPercentages) {
    this.rolloutPercentages = Map.copyOf(rolloutPercentages);
  }
}
