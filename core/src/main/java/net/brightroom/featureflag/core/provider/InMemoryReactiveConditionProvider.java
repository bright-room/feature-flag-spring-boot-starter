package net.brightroom.featureflag.core.provider;

import java.util.Map;
import reactor.core.publisher.Mono;

/**
 * An implementation of {@link ReactiveConditionProvider} that stores condition expressions in
 * memory using a {@link Map}.
 *
 * <p>This class provides a simple, immutable in-memory mechanism to resolve condition expressions
 * reactively. When a feature has no configured condition, an empty {@link Mono} is returned.
 */
public class InMemoryReactiveConditionProvider implements ReactiveConditionProvider {

  private final Map<String, String> conditions;

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

  /**
   * Constructs an instance with the provided condition expressions.
   *
   * @param conditions a map containing feature flag names as keys and their condition expressions
   *     as values; copied defensively on construction
   */
  public InMemoryReactiveConditionProvider(Map<String, String> conditions) {
    this.conditions = Map.copyOf(conditions);
  }
}
