package net.brightroom.featureflag.core.provider;

import java.util.Map;
import java.util.Optional;
import net.brightroom.featureflag.core.properties.ScheduleConfiguration;

/**
 * An implementation of {@link ScheduleProvider} that stores schedule configurations in memory using
 * a {@link Map}.
 *
 * <p>This class provides a simple, immutable in-memory mechanism to resolve feature flag schedules.
 * When a feature has no configured schedule, {@link Optional#empty()} is returned.
 */
public class InMemoryScheduleProvider implements ScheduleProvider {

  private final Map<String, ScheduleConfiguration> schedules;

  /**
   * {@inheritDoc}
   *
   * <p>Returns {@link Optional#empty()} for features not present in the schedule map.
   */
  @Override
  public Optional<ScheduleConfiguration> getSchedule(String featureName) {
    return Optional.ofNullable(schedules.get(featureName));
  }

  /**
   * Constructs an instance with the provided schedule configurations.
   *
   * @param schedules a map containing feature flag names as keys and their schedule configurations
   *     as values; copied defensively on construction
   */
  public InMemoryScheduleProvider(Map<String, ScheduleConfiguration> schedules) {
    this.schedules = Map.copyOf(schedules);
  }
}
