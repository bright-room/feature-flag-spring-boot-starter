package net.brightroom.featureflag.actuator.health;

import java.util.Map;
import reactor.core.publisher.Mono;

/**
 * Reactive SPI for contributing additional details to the {@link
 * ReactiveFeatureFlagHealthIndicator}.
 *
 * <p>Implement this interface and register the implementation as a Spring bean to include custom
 * details in the {@code featureFlag} health indicator response for reactive applications. This is
 * useful for custom providers (e.g., DB- or Redis-backed) that want to expose additional
 * information such as connection pool status or latency.
 *
 * <p>Example:
 *
 * <pre>{@code
 * @Component
 * public class MyProviderReactiveHealthDetailsContributor implements ReactiveHealthDetailsContributor {
 *
 *   @Override
 *   public Mono<Map<String, Object>> contributeDetails() {
 *     return Mono.just(Map.of("connectionPoolSize", 10, "latencyMs", 5));
 *   }
 * }
 * }</pre>
 *
 * <p>All registered contributors are called during the health check, and their returned details are
 * merged into the health response. If two contributors return the same key, the last one wins.
 */
public interface ReactiveHealthDetailsContributor {

  /**
   * Returns a {@link Mono} emitting a map of additional details to include in the health response.
   *
   * @return a {@link Mono} emitting a non-null map of detail key-value pairs; may be empty
   */
  Mono<Map<String, Object>> contributeDetails();
}
