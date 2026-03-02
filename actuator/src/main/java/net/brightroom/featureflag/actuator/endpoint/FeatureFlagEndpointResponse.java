package net.brightroom.featureflag.actuator.endpoint;

import java.util.Map;

/**
 * Response payload for the {@code /actuator/feature-flags} endpoint.
 *
 * <p>Wraps all currently known feature flags and their enabled states.
 */
public record FeatureFlagEndpointResponse(Map<String, Boolean> features) {}
