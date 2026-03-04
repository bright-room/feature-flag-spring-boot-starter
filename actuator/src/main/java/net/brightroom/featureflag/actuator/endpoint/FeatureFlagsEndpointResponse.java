package net.brightroom.featureflag.actuator.endpoint;

import java.util.List;

/**
 * Response body for the {@code /actuator/feature-flags} endpoint.
 *
 * <p>Contains a snapshot of all feature flags and the default-enabled policy at the time of the
 * request.
 *
 * @param features a list of all feature flags and their current enabled states
 * @param defaultEnabled the fallback value returned for flags not present in {@code features}
 */
public record FeatureFlagsEndpointResponse(
    List<FeatureFlagEndpointResponse> features, boolean defaultEnabled) {}
