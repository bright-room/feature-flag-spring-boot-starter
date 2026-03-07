package net.brightroom.featureflag.core.provider;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import reactor.core.publisher.Mono;

/**
 * A thread-safe, in-memory implementation of {@link MutableReactiveConditionProvider}.
 *
 * <p>Condition expressions are stored in a {@link ConcurrentHashMap}, allowing concurrent reads and
 * writes without external synchronization.
 */
public class MutableInMemoryReactiveConditionProvider implements MutableReactiveConditionProvider {

  private final ConcurrentHashMap<String, String> conditions;

  /**
   * {@inheritDoc}
   *
   * <p>Returns an empty {@link Mono} for features not present in the conditions map.
   */
  @Override
  public Mono<String> getCondition(String featureName) {
    String condition = conditions.get(featureName);
    return condition != null ? Mono.just(condition) : Mono.empty();
  }

  /** {@inheritDoc} */
  @Override
  public Mono<Map<String, String>> getConditions() {
    return Mono.just(Map.copyOf(conditions));
  }

  /** {@inheritDoc} */
  @Override
  public Mono<Void> setCondition(String featureName, String condition) {
    return Mono.<Void>fromRunnable(() -> conditions.put(featureName, condition));
  }

  /** {@inheritDoc} */
  @Override
  public Mono<Boolean> removeCondition(String featureName) {
    return Mono.fromCallable(() -> conditions.remove(featureName) != null);
  }

  /**
   * Constructs a {@code MutableInMemoryReactiveConditionProvider} with the given initial condition
   * expressions.
   *
   * @param conditions the initial condition expressions map; copied defensively on construction
   */
  public MutableInMemoryReactiveConditionProvider(Map<String, String> conditions) {
    this.conditions = new ConcurrentHashMap<>(conditions);
  }
}
