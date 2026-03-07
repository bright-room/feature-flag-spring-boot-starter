package net.brightroom.featureflag.actuator.health;

import java.util.Map;

/**
 * SPI for contributing additional details to the {@link FeatureFlagHealthIndicator}.
 *
 * <p>Implement this interface and register the implementation as a Spring bean to include custom
 * details in the {@code featureFlag} health indicator response. This is useful for custom providers
 * (e.g., DB- or Redis-backed) that want to expose additional information such as connection pool
 * status or latency.
 *
 * <p>Example:
 *
 * <pre>{@code
 * @Component
 * public class MyProviderHealthDetailsContributor implements HealthDetailsContributor {
 *
 *   @Override
 *   public Map<String, Object> contributeDetails() {
 *     return Map.of("connectionPoolSize", 10, "latencyMs", 5);
 *   }
 * }
 * }</pre>
 *
 * <p>All registered contributors are called during the health check, and their returned details are
 * merged into the health response. If two contributors return the same key, the last one wins.
 */
public interface HealthDetailsContributor {

  /**
   * Returns a map of additional details to include in the health response.
   *
   * @return a non-null map of detail key-value pairs; may be empty
   */
  Map<String, Object> contributeDetails();
}
