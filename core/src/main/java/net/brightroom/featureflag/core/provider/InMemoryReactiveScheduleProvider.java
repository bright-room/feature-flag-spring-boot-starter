package net.brightroom.featureflag.core.provider;

import java.util.Map;
import net.brightroom.featureflag.core.properties.ScheduleConfiguration;
import reactor.core.publisher.Mono;

/**
 * An implementation of {@link ReactiveScheduleProvider} that stores schedule configurations in
 * memory using a {@link Map}.
 *
 * <p>This class provides a simple, immutable in-memory mechanism to resolve feature flag schedules
 * reactively. When a feature has no configured schedule, an empty {@link Mono} is returned.
 */
public class InMemoryReactiveScheduleProvider implements ReactiveScheduleProvider {

  private final Map<String, ScheduleConfiguration> schedules;

  /**
   * {@inheritDoc}
   *
   * <p>Returns an empty {@link Mono} for features not present in the schedule map.
   */
  @Override
  public Mono<ScheduleConfiguration> getSchedule(String featureName) {
    return Mono.justOrEmpty(schedules.get(featureName));
  }

  /**
   * Constructs an instance with the provided schedule configurations.
   *
   * @param schedules a map containing feature flag names as keys and their schedule configurations
   *     as values; copied defensively on construction
   */
  public InMemoryReactiveScheduleProvider(Map<String, ScheduleConfiguration> schedules) {
    this.schedules = Map.copyOf(schedules);
  }
}
