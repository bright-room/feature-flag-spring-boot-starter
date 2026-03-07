package net.brightroom.featureflag.core.provider;

import java.util.Map;
import java.util.Optional;

/**
 * An implementation of {@link ConditionProvider} that stores condition expressions in memory using
 * a {@link Map}.
 *
 * <p>This class provides a simple, immutable in-memory mechanism to resolve condition expressions.
 * When a feature has no configured condition, {@link Optional#empty()} is returned.
 */
public class InMemoryConditionProvider implements ConditionProvider {

  private final Map<String, String> conditions;

  /**
   * {@inheritDoc}
   *
   * <p>Returns {@link Optional#empty()} for features not present in the conditions map.
   */
  @Override
  public Optional<String> getCondition(String featureName) {
    return Optional.ofNullable(conditions.get(featureName));
  }

  /**
   * Constructs an instance with the provided condition expressions.
   *
   * @param conditions a map containing feature flag names as keys and their condition expressions
   *     as values; copied defensively on construction
   */
  public InMemoryConditionProvider(Map<String, String> conditions) {
    this.conditions = Map.copyOf(conditions);
  }
}
