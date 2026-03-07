package net.brightroom.featureflag.core.provider;

import java.util.Optional;

/**
 * SPI for resolving the SpEL condition expression for a given feature flag.
 *
 * <p>Implementations provide the configured condition expression for each feature flag. When a
 * feature has no configured condition, {@link Optional#empty()} is returned and the feature is
 * treated as having no condition restriction.
 *
 * <p>Implement this interface and register it as a Spring bean to override the default in-memory
 * provider. For example, to read conditions from a database or remote config service.
 */
public interface ConditionProvider {

  /**
   * Returns the configured condition expression for the specified feature.
   *
   * @param featureName the name of the feature flag
   * @return an {@link Optional} containing the SpEL condition expression, or {@link
   *     Optional#empty()} if no condition is configured for this feature
   */
  Optional<String> getCondition(String featureName);
}
