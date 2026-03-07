package net.brightroom.featureflag.core.provider;

import reactor.core.publisher.Mono;

/**
 * Reactive SPI for resolving the SpEL condition expression for a given feature flag.
 *
 * <p>Implementations provide the configured condition expression for each feature flag. When a
 * feature has no configured condition, an empty {@link Mono} is returned and the feature is treated
 * as having no condition restriction.
 *
 * <p>Implement this interface and register it as a Spring bean to override the default in-memory
 * provider. For example, to read conditions from a reactive data source.
 */
public interface ReactiveConditionProvider {

  /**
   * Returns the configured condition expression for the specified feature.
   *
   * @param featureName the name of the feature flag
   * @return a {@link Mono} emitting the SpEL condition expression, or an empty {@link Mono} if no
   *     condition is configured for this feature
   */
  Mono<String> getCondition(String featureName);
}
