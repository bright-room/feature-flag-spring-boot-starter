package net.brightroom.featureflag.core.provider;

import java.util.Map;

/**
 * An extension of {@link ConditionProvider} that supports dynamic mutation of condition expressions
 * at runtime.
 *
 * <p>Implementations must be thread-safe, as conditions may be read and updated concurrently.
 *
 * <p>This interface serves as an SPI for the actuator endpoint to update condition expressions at
 * runtime without restarting the application.
 */
public interface MutableConditionProvider extends ConditionProvider {

  /**
   * Returns a snapshot of all currently configured condition expressions.
   *
   * <p>The returned map must be an immutable copy; mutations to the returned map must not affect
   * the provider's internal state.
   *
   * @return an immutable map of feature flag names to their condition expressions
   */
  Map<String, String> getConditions();

  /**
   * Updates the condition expression for the specified feature flag.
   *
   * <p>If the feature flag does not have a configured condition, it is created.
   *
   * @param featureName the name of the feature flag to update
   * @param condition the new condition expression; must not be null
   */
  void setCondition(String featureName, String condition);

  /**
   * Removes the condition expression for the specified feature flag.
   *
   * <p>If the feature flag does not have a configured condition, this method is a no-op.
   *
   * @param featureName the name of the feature flag whose condition to remove
   */
  void removeCondition(String featureName);
}
