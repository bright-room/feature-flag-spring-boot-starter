package net.brightroom.featureflag.actuator.endpoint;

import java.util.List;
import java.util.Map;
import net.brightroom.featureflag.core.event.FeatureFlagChangedEvent;
import net.brightroom.featureflag.core.provider.MutableReactiveFeatureFlagProvider;
import net.brightroom.featureflag.core.provider.MutableReactiveRolloutPercentageProvider;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.actuate.endpoint.Access;
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
    var enabled = provider.isFeatureEnabled(featureName).block();
    var rollout = rolloutProvider.getRolloutPercentage(featureName).blockOptional().orElse(100);
    return new FeatureFlagEndpointResponse(featureName, Boolean.TRUE.equals(enabled), rollout);
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
    provider.setFeatureEnabled(featureName, enabled).block();
    if (rollout != null) {
      if (rollout < 0 || rollout > 100) {
        throw new IllegalArgumentException(
            "rollout must be between 0 and 100, but was: " + rollout);
      }
      rolloutProvider.setRolloutPercentage(featureName, rollout);
    }
    eventPublisher.publishEvent(new FeatureFlagChangedEvent(this, featureName, enabled, rollout));
    return buildFlagsResponse();
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
            .map(
                e ->
                    new FeatureFlagEndpointResponse(
                        e.getKey(), e.getValue(), rolloutPercentages.getOrDefault(e.getKey(), 100)))
            .toList();
    return new FeatureFlagsEndpointResponse(featureList, defaultEnabled);
  }

  /**
   * Constructs a {@code ReactiveFeatureFlagEndpoint}.
   *
   * @param provider the mutable reactive feature flag provider
   * @param rolloutProvider the mutable reactive rollout percentage provider
   * @param defaultEnabled the default-enabled value to include in responses
   * @param eventPublisher the publisher used to broadcast flag change events
   */
  public ReactiveFeatureFlagEndpoint(
      MutableReactiveFeatureFlagProvider provider,
      MutableReactiveRolloutPercentageProvider rolloutProvider,
      boolean defaultEnabled,
      ApplicationEventPublisher eventPublisher) {
    this.provider = provider;
    this.rolloutProvider = rolloutProvider;
    this.defaultEnabled = defaultEnabled;
    this.eventPublisher = eventPublisher;
  }
}
