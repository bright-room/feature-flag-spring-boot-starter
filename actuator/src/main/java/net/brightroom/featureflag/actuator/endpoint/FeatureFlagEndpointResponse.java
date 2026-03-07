package net.brightroom.featureflag.actuator.endpoint;

import org.jspecify.annotations.Nullable;

/**
 * Response body for the {@code /actuator/feature-flags/{featureName}} endpoint.
 *
 * <p>Contains the state of a single feature flag at the time of the request.
 *
 * @param featureName the name of the feature flag
 * @param enabled the current enabled state of the feature flag
 * @param rollout the current rollout percentage (0–100) of the feature flag
 * @param schedule the schedule configuration for this flag, or {@code null} if no schedule is
 *     configured
 */
public record FeatureFlagEndpointResponse(
    String featureName, boolean enabled, int rollout, @Nullable ScheduleEndpointResponse schedule) {

  /**
   * Creates a response without schedule information.
   *
   * @param featureName the name of the feature flag
   * @param enabled the current enabled state of the feature flag
   * @param rollout the current rollout percentage (0–100) of the feature flag
   */
  public FeatureFlagEndpointResponse(String featureName, boolean enabled, int rollout) {
    this(featureName, enabled, rollout, null);
  }
}
