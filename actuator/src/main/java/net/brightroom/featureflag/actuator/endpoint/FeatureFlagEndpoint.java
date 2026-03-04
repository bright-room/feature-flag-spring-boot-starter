package net.brightroom.featureflag.actuator.endpoint;

import net.brightroom.featureflag.core.event.FeatureFlagChangedEvent;
import net.brightroom.featureflag.core.event.FeatureFlagRemovedEvent;
import net.brightroom.featureflag.core.provider.MutableFeatureFlagProvider;
import net.brightroom.featureflag.core.provider.MutableRolloutPercentageProvider;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.actuate.endpoint.Access;
import org.springframework.boot.actuate.endpoint.annotation.DeleteOperation;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.Selector;
import org.springframework.boot.actuate.endpoint.annotation.WriteOperation;
import org.springframework.context.ApplicationEventPublisher;

/**
 * Spring Boot Actuator endpoint for runtime feature flag management.
 *
 * <p>Exposed at {@code /actuator/feature-flags}. Allows reading and updating feature flag states
 * without restarting the application.
 *
 * <p>This endpoint is only registered when a {@link MutableFeatureFlagProvider} bean is present in
 * the application context (see {@code FeatureFlagActuatorAutoConfiguration}).
 *
 * <p>By default, both read and write operations are unrestricted. In production, consider
 * restricting access via {@code management.endpoint.feature-flags.access=READ_ONLY} or securing the
 * endpoint with Spring Security.
 */
@Endpoint(id = "feature-flags", defaultAccess = Access.UNRESTRICTED)
public class FeatureFlagEndpoint {

  private final MutableFeatureFlagProvider provider;
  private final MutableRolloutPercentageProvider rolloutProvider;
  private final boolean defaultEnabled;
  private final ApplicationEventPublisher eventPublisher;

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
    return new FeatureFlagEndpointResponse(
        featureName,
        provider.isFeatureEnabled(featureName),
        rolloutProvider.getRolloutPercentage(featureName).orElse(100));
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
    provider.setFeatureEnabled(featureName, enabled);
    if (rollout != null) {
      rolloutProvider.setRolloutPercentage(featureName, rollout);
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
    boolean removed = provider.removeFeature(featureName);
    rolloutProvider.removeRolloutPercentage(featureName);
    if (removed) {
      eventPublisher.publishEvent(new FeatureFlagRemovedEvent(this, featureName));
    }
  }

  private FeatureFlagsEndpointResponse buildFlagsResponse() {
    var rolloutPercentages = rolloutProvider.getRolloutPercentages();
    var featureList =
        provider.getFeatures().entrySet().stream()
            .map(
                e ->
                    new FeatureFlagEndpointResponse(
                        e.getKey(), e.getValue(), rolloutPercentages.getOrDefault(e.getKey(), 100)))
            .toList();
    return new FeatureFlagsEndpointResponse(featureList, defaultEnabled);
  }

  /**
   * Constructs a {@code FeatureFlagEndpoint}.
   *
   * @param provider the mutable feature flag provider
   * @param rolloutProvider the mutable rollout percentage provider
   * @param defaultEnabled the default-enabled value to include in responses
   * @param eventPublisher the publisher used to broadcast flag change events
   */
  public FeatureFlagEndpoint(
      MutableFeatureFlagProvider provider,
      MutableRolloutPercentageProvider rolloutProvider,
      boolean defaultEnabled,
      ApplicationEventPublisher eventPublisher) {
    this.provider = provider;
    this.rolloutProvider = rolloutProvider;
    this.defaultEnabled = defaultEnabled;
    this.eventPublisher = eventPublisher;
  }
}
