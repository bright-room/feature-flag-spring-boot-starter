package net.brightroom.featureflag.webflux.context;

import net.brightroom.featureflag.core.context.FeatureFlagContext;
import org.springframework.http.server.reactive.ServerHttpRequest;
import reactor.core.publisher.Mono;

/**
 * Resolves the feature flag context from the current reactive HTTP request.
 *
 * <p>The context is used for rollout percentage checks. The default implementation ({@code
 * RandomReactiveFeatureFlagContextResolver}) generates a random UUID per request, providing
 * non-sticky (per-request probabilistic) rollout behavior.
 *
 * <p>To achieve sticky rollout (same user always gets the same result), implement this interface
 * and register it as a {@code @Bean}. The custom bean will replace the default due to
 * {@code @ConditionalOnMissingBean}.
 *
 * <p>Return {@link Mono#empty()} if the identifier cannot be resolved. In that case, the rollout
 * check is skipped and the feature is treated as fully enabled (fail-open).
 */
public interface ReactiveFeatureFlagContextResolver {

  /**
   * Resolves the feature flag context from the current request.
   *
   * @param request the current reactive HTTP request
   * @return a {@link Mono} emitting the resolved context, or empty to skip the rollout check
   */
  Mono<FeatureFlagContext> resolve(ServerHttpRequest request);
}
