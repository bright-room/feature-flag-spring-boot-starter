package net.brightroom.featureflag.actuator.endpoint;

import net.brightroom.featureflag.core.event.FeatureFlagChangedEvent;
import net.brightroom.featureflag.core.provider.MutableReactiveFeatureFlagProvider;
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
  private final boolean defaultEnabled;
  private final ApplicationEventPublisher eventPublisher;

  /**
   * Returns the current state of all feature flags.
   *
   * @return a response containing all flags and the default-enabled policy
   */
  @ReadOperation
  public FeatureFlagsEndpointResponse features() {
    var features = provider.getFeatures().block();
    var featureList =
        features.entrySet().stream()
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
    var enabled = provider.isFeatureEnabled(featureName).block();
    return new FeatureFlagEndpointResponse(featureName, enabled);
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
    provider.setFeatureEnabled(featureName, enabled).block();
    eventPublisher.publishEvent(new FeatureFlagChangedEvent(this, featureName, enabled));
    var features = provider.getFeatures().block();
    var featureList =
        features.entrySet().stream()
            .map(e -> new FeatureFlagEndpointResponse(e.getKey(), e.getValue()))
            .toList();
    return new FeatureFlagsEndpointResponse(featureList, defaultEnabled);
  }

  /**
   * Constructs a {@code ReactiveFeatureFlagEndpoint}.
   *
   * @param provider the mutable reactive feature flag provider
   * @param defaultEnabled the default-enabled value to include in responses
   * @param eventPublisher the publisher used to broadcast flag change events
   */
  public ReactiveFeatureFlagEndpoint(
      MutableReactiveFeatureFlagProvider provider,
      boolean defaultEnabled,
      ApplicationEventPublisher eventPublisher) {
    this.provider = provider;
    this.defaultEnabled = defaultEnabled;
    this.eventPublisher = eventPublisher;
  }
}
