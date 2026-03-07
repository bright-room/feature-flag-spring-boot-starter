package net.brightroom.featureflag.core.provider;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A thread-safe, in-memory implementation of {@link MutableConditionProvider}.
 *
 * <p>Condition expressions are stored in a {@link ConcurrentHashMap}, allowing concurrent reads and
 * writes without external synchronization.
 */
public class MutableInMemoryConditionProvider implements MutableConditionProvider {

  private final ConcurrentHashMap<String, String> conditions;

  /**
   * {@inheritDoc}
   *
   * <p>Returns {@link Optional#empty()} for features not present in the conditions map.
   */
  @Override
  public Optional<String> getCondition(String featureName) {
    return Optional.ofNullable(conditions.get(featureName));
  }

  /** {@inheritDoc} */
  @Override
  public Map<String, String> getConditions() {
    return Map.copyOf(conditions);
  }

  /** {@inheritDoc} */
  @Override
  public void setCondition(String featureName, String condition) {
    conditions.put(featureName, condition);
  }

  /** {@inheritDoc} */
  @Override
  public void removeCondition(String featureName) {
    conditions.remove(featureName);
  }

  /**
   * Constructs a {@code MutableInMemoryConditionProvider} with the given initial condition
   * expressions.
   *
   * @param conditions the initial condition expressions map; copied defensively on construction
   */
  public MutableInMemoryConditionProvider(Map<String, String> conditions) {
    this.conditions = new ConcurrentHashMap<>(conditions);
  }
}
