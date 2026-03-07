package net.brightroom.featureflag.core.provider;

import reactor.core.publisher.Mono;

/**
 * Reactive SPI for resolving the schedule configuration for a given feature flag.
 *
 * <p>Implementations provide the configured schedule for each feature flag. When a feature has no
 * configured schedule, an empty {@link Mono} is returned and the caller treats the schedule as
 * always active.
 *
 * <p>Implement this interface and register it as a Spring bean to override the default in-memory
 * provider. For example, to read schedules from a reactive data source.
 */
public interface ReactiveScheduleProvider {

  /**
   * Returns the configured schedule for the specified feature.
   *
   * @param featureName the name of the feature flag
   * @return a {@link Mono} emitting the {@link Schedule}, or an empty {@link Mono} if no schedule
   *     is configured for this feature
   */
  Mono<Schedule> getSchedule(String featureName);
}
