package net.brightroom.featureflag.actuator.endpoint;

import net.brightroom.featureflag.core.event.FeatureFlagChangedEvent;
import net.brightroom.featureflag.core.provider.MutableFeatureFlagProvider;
import org.springframework.boot.actuate.endpoint.Access;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
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
  public FeatureFlagEndpointResponse features() {
    return new FeatureFlagEndpointResponse(provider.getFeatures(), defaultEnabled);
  }

  /**
   * Updates the enabled state of a feature flag and publishes a {@link FeatureFlagChangedEvent}.
   *
   * <p>If the flag does not exist, it is created with the given state.
   *
   * @param featureName the name of the feature flag to update
   * @param enabled the new enabled state
   * @return a response reflecting the updated state of all flags
   */
  @WriteOperation
  public FeatureFlagEndpointResponse updateFeature(String featureName, boolean enabled) {
    provider.setFeatureEnabled(featureName, enabled);
    eventPublisher.publishEvent(new FeatureFlagChangedEvent(this, featureName, enabled));
    return new FeatureFlagEndpointResponse(provider.getFeatures(), defaultEnabled);
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
