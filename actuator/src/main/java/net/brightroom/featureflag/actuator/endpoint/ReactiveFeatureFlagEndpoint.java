package net.brightroom.featureflag.actuator.endpoint;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import net.brightroom.featureflag.core.event.FeatureFlagChangedEvent;
import net.brightroom.featureflag.core.event.FeatureFlagRemovedEvent;
import net.brightroom.featureflag.core.provider.MutableReactiveFeatureFlagProvider;
import net.brightroom.featureflag.core.provider.MutableReactiveRolloutPercentageProvider;
import net.brightroom.featureflag.core.provider.ReactiveScheduleProvider;
import net.brightroom.featureflag.core.provider.Schedule;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.actuate.endpoint.Access;
import org.springframework.boot.actuate.endpoint.annotation.DeleteOperation;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.Selector;
import org.springframework.boot.actuate.endpoint.annotation.WriteOperation;
import org.springframework.context.ApplicationEventPublisher;

/**
 * Spring Boot Actuator endpoint for runtime feature flag management in reactive applications.
 *
 * <p>Exposed at {@code /actuator/feature-flags}. Allows reading and updating feature flag states
 * without restarting the application.
 *
 * <p>This endpoint delegates to a {@link MutableReactiveFeatureFlagProvider} and blocks on the
 * reactive operations. This is safe because actuator endpoints run on the management thread pool,
 * not on the event loop.
 *
 * <p>By default, both read and write operations are unrestricted. In production, consider
 * restricting access via {@code management.endpoint.feature-flags.access=READ_ONLY} or securing the
 * endpoint with Spring Security.
 */
@Endpoint(id = "feature-flags", defaultAccess = Access.UNRESTRICTED)
public class ReactiveFeatureFlagEndpoint {

  private final MutableReactiveFeatureFlagProvider provider;
  private final MutableReactiveRolloutPercentageProvider rolloutProvider;
  private final ReactiveScheduleProvider reactiveScheduleProvider;
  private final boolean defaultEnabled;
  private final ApplicationEventPublisher eventPublisher;
  private final Clock clock;

  /**
   * Returns the current state of all feature flags.
   *
   * @return a response containing all flags and the default-enabled policy
   */
  @ReadOperation
  public FeatureFlagsEndpointResponse features() {
    return buildFlagsResponse();
  }

  /**
   * Returns the current state of a single feature flag.
   *
   * <p>If the flag is not defined, the response reflects the {@code defaultEnabled} policy.
   *
   * @param featureName the name of the feature flag to read
   * @return a response containing the flag name and its current enabled state
   */
  @ReadOperation
  public FeatureFlagEndpointResponse feature(@Selector String featureName) {
    if (featureName == null || featureName.isBlank()) {
      throw new IllegalArgumentException("featureName must not be null or blank");
    }
    var enabled = provider.isFeatureEnabled(featureName).block();
    var rollout = rolloutProvider.getRolloutPercentage(featureName).blockOptional().orElse(100);
    return new FeatureFlagEndpointResponse(
        featureName, Boolean.TRUE.equals(enabled), rollout, buildScheduleResponse(featureName));
  }

  /**
   * Updates the enabled state and optionally the rollout percentage of a feature flag, then
   * publishes a {@link FeatureFlagChangedEvent}.
   *
   * <p>If the flag does not exist, it is created with the given state.
   *
   * <p><b>Note:</b> {@link FeatureFlagChangedEvent} is published on every invocation, regardless of
   * whether the value actually changed.
   *
   * @param featureName the name of the feature flag to update
   * @param enabled the new enabled state
   * @param rollout the new rollout percentage (0–100), or {@code null} to leave unchanged
   * @return a response reflecting the updated state of all flags
   */
  @WriteOperation
  public FeatureFlagsEndpointResponse updateFeature(
      String featureName, boolean enabled, @Nullable Integer rollout) {
    if (featureName == null || featureName.isBlank()) {
      throw new IllegalArgumentException("featureName must not be null or blank");
    }
    if (rollout != null && (rollout < 0 || rollout > 100)) {
      throw new IllegalArgumentException("rollout must be between 0 and 100, but was: " + rollout);
    }
    provider.setFeatureEnabled(featureName, enabled).block();
    if (rollout != null) {
      rolloutProvider.setRolloutPercentage(featureName, rollout).block();
    }
    eventPublisher.publishEvent(new FeatureFlagChangedEvent(this, featureName, enabled, rollout));
    return buildFlagsResponse();
  }

  /**
   * Removes a feature flag and its associated rollout percentage.
   *
   * <p>A {@link FeatureFlagRemovedEvent} is published only if the flag actually existed. This
   * operation is idempotent: deleting a non-existent flag is a no-op and still returns 204 No
   * Content without publishing an event.
   *
   * @param featureName the name of the feature flag to remove
   * @throws IllegalArgumentException if {@code featureName} is {@code null} or blank
   */
  @DeleteOperation
  public void deleteFeature(@Selector String featureName) {
    if (featureName == null || featureName.isBlank()) {
      throw new IllegalArgumentException("featureName must not be null or blank");
    }
    Boolean removed = provider.removeFeature(featureName).block();
    rolloutProvider.removeRolloutPercentage(featureName).block();
    if (Boolean.TRUE.equals(removed)) {
      eventPublisher.publishEvent(new FeatureFlagRemovedEvent(this, featureName));
    }
  }

  private FeatureFlagsEndpointResponse buildFlagsResponse() {
    var features = provider.getFeatures().block();
    if (features == null) {
      return new FeatureFlagsEndpointResponse(List.of(), defaultEnabled);
    }
    var rolloutPercentages =
        rolloutProvider.getRolloutPercentages().blockOptional().orElse(Map.of());
    var featureList =
        features.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .map(
                e ->
                    new FeatureFlagEndpointResponse(
                        e.getKey(),
                        e.getValue(),
                        rolloutPercentages.getOrDefault(e.getKey(), 100),
                        buildScheduleResponse(e.getKey())))
            .toList();
    return new FeatureFlagsEndpointResponse(featureList, defaultEnabled);
  }

  @Nullable
  private ScheduleEndpointResponse buildScheduleResponse(String featureName) {
    Schedule schedule =
        reactiveScheduleProvider.getSchedule(featureName).blockOptional().orElse(null);
    if (schedule == null) {
      return null;
    }
    return new ScheduleEndpointResponse(
        schedule.start(), schedule.end(), schedule.timezone(), schedule.isActive(clock.instant()));
  }

  /**
   * Constructs a {@code ReactiveFeatureFlagEndpoint}.
   *
   * @param provider the mutable reactive feature flag provider
   * @param rolloutProvider the mutable reactive rollout percentage provider
   * @param reactiveScheduleProvider the reactive schedule provider used to look up schedules per
   *     feature
   * @param defaultEnabled the default-enabled value to include in responses
   * @param eventPublisher the publisher used to broadcast flag change events
   * @param clock the clock used to determine schedule active status in responses
   */
  public ReactiveFeatureFlagEndpoint(
      MutableReactiveFeatureFlagProvider provider,
      MutableReactiveRolloutPercentageProvider rolloutProvider,
      ReactiveScheduleProvider reactiveScheduleProvider,
      boolean defaultEnabled,
      ApplicationEventPublisher eventPublisher,
      Clock clock) {
    this.provider = provider;
    this.rolloutProvider = rolloutProvider;
    this.reactiveScheduleProvider = reactiveScheduleProvider;
    this.defaultEnabled = defaultEnabled;
    this.eventPublisher = eventPublisher;
    this.clock = clock;
  }
}
