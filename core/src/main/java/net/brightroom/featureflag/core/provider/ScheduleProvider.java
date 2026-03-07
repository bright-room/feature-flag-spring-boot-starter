package net.brightroom.featureflag.core.provider;

import java.util.Optional;

/**
 * SPI for resolving the schedule configuration for a given feature flag.
 *
 * <p>Implementations provide the configured schedule for each feature flag. When a feature has no
 * configured schedule, {@link Optional#empty()} is returned and the caller treats the schedule as
 * always active.
 *
 * <p>Implement this interface and register it as a Spring bean to override the default in-memory
 * provider. For example, to read schedules from a database or remote config service.
 */
public interface ScheduleProvider {

  /**
   * Returns the configured schedule for the specified feature.
   *
   * @param featureName the name of the feature flag
   * @return an {@link Optional} containing the {@link Schedule}, or {@link Optional#empty()} if no
   *     schedule is configured for this feature
   */
  Optional<Schedule> getSchedule(String featureName);
}
