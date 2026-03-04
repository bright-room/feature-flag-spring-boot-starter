package net.brightroom.featureflag.actuator.endpoint;

/**
 * Response body for the {@code /actuator/feature-flags/{featureName}} endpoint.
 *
 * <p>Contains the state of a single feature flag at the time of the request.
 *
 * @param featureName the name of the feature flag
 * @param enabled the current enabled state of the feature flag
 * @param rollout the current rollout percentage (0–100) of the feature flag
 */
public record FeatureFlagEndpointResponse(String featureName, boolean enabled, int rollout) {}
