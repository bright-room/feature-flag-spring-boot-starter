package net.brightroom.featureflag.actuator.endpoint;

import net.brightroom.featureflag.core.event.FeatureFlagChangedEvent;
import net.brightroom.featureflag.core.provider.MutableFeatureFlagProvider;
import org.springframework.boot.actuate.endpoint.Access;
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
  private final boolean defaultEnabled;
  private final ApplicationEventPublisher eventPublisher;

  /**
   * Returns the current state of all feature flags.
   *
   * @return a response containing all flags and the default-enabled policy
   */
  @ReadOperation
  public FeatureFlagsEndpointResponse features() {
    var featureList =
        provider.getFeatures().entrySet().stream()
            .map(e -> new FeatureFlagEndpointResponse(e.getKey(), e.getValue()))
            .toList();
    return new FeatureFlagsEndpointResponse(featureList, defaultEnabled);
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
    return new FeatureFlagEndpointResponse(featureName, provider.isFeatureEnabled(featureName));
  }

  /**
   * Updates the enabled state of a feature flag and publishes a {@link FeatureFlagChangedEvent}.
   *
   * <p>If the flag does not exist, it is created with the given state.
   *
   * <p><b>Note:</b> {@link FeatureFlagChangedEvent} is published on every invocation, regardless of
   * whether the value actually changed.
   *
   * @param featureName the name of the feature flag to update
   * @param enabled the new enabled state
   * @return a response reflecting the updated state of all flags
   */
  @WriteOperation
  public FeatureFlagsEndpointResponse updateFeature(String featureName, boolean enabled) {
    if (featureName == null || featureName.isBlank()) {
      throw new IllegalArgumentException("featureName must not be null or blank");
    }
    provider.setFeatureEnabled(featureName, enabled);
    eventPublisher.publishEvent(new FeatureFlagChangedEvent(this, featureName, enabled));
    var featureList =
        provider.getFeatures().entrySet().stream()
            .map(e -> new FeatureFlagEndpointResponse(e.getKey(), e.getValue()))
            .toList();
    return new FeatureFlagsEndpointResponse(featureList, defaultEnabled);
  }

  /**
   * Constructs a {@code FeatureFlagEndpoint}.
   *
   * @param provider the mutable feature flag provider
   * @param defaultEnabled the default-enabled value to include in responses
   * @param eventPublisher the publisher used to broadcast flag change events
   */
  public FeatureFlagEndpoint(
      MutableFeatureFlagProvider provider,
      boolean defaultEnabled,
      ApplicationEventPublisher eventPublisher) {
    this.provider = provider;
    this.defaultEnabled = defaultEnabled;
    this.eventPublisher = eventPublisher;
  }
}
