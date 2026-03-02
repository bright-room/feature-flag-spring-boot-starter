package net.brightroom.featureflag.actuator.endpoint;

import net.brightroom.featureflag.core.provider.MutableFeatureFlagProvider;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.Selector;
import org.springframework.boot.actuate.endpoint.annotation.WriteOperation;

/**
 * Spring Boot Actuator endpoint for managing feature flags at runtime.
 *
 * <p>Exposed at {@code /actuator/feature-flags} when the endpoint is enabled and configured for web
 * exposure (e.g., {@code management.endpoints.web.exposure.include=feature-flags}).
 *
 * <h2>Operations</h2>
 *
 * <ul>
 *   <li>{@code GET /actuator/feature-flags} — returns all feature flags and their current states
 *   <li>{@code POST /actuator/feature-flags/{featureName}} with body {@code {"enabled":
 *       true|false}} — enables or disables the specified feature flag at runtime
 * </ul>
 *
 * <p>Requires a {@link MutableFeatureFlagProvider} bean in the application context. The default
 * {@link net.brightroom.featureflag.core.provider.InMemoryMutableFeatureFlagProvider} is
 * auto-configured when the {@code actuator} module is on the classpath.
 */
@Endpoint(id = "feature-flags")
public class FeatureFlagEndpoint {

  private final MutableFeatureFlagProvider provider;

  public FeatureFlagEndpoint(MutableFeatureFlagProvider provider) {
    this.provider = provider;
  }

  /**
   * Returns all currently known feature flags and their enabled states.
   *
   * @return a response containing all feature flags
   */
  @ReadOperation
  public FeatureFlagEndpointResponse getAll() {
    return new FeatureFlagEndpointResponse(provider.getAllFeatures());
  }

  /**
   * Enables or disables the specified feature flag at runtime.
   *
   * @param featureName the name of the feature flag to update
   * @param enabled {@code true} to enable the feature, {@code false} to disable it
   */
  @WriteOperation
  public void update(@Selector String featureName, boolean enabled) {
    if (enabled) {
      provider.enable(featureName);
    } else {
      provider.disable(featureName);
    }
  }
}
