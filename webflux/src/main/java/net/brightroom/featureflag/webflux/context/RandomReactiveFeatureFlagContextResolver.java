package net.brightroom.featureflag.webflux.context;

import java.util.UUID;
import net.brightroom.featureflag.core.context.FeatureFlagContext;
import org.springframework.http.server.reactive.ServerHttpRequest;
import reactor.core.publisher.Mono;

/**
 * Default {@link ReactiveFeatureFlagContextResolver} that generates a random UUID per request.
 *
 * <p>This provides non-sticky (per-request probabilistic) rollout behavior. Each request
 * independently has a rollout-percentage chance of being included.
 *
 * <p>This class is package-private. Users interact with {@link ReactiveFeatureFlagContextResolver}
 * only.
 */
class RandomReactiveFeatureFlagContextResolver implements ReactiveFeatureFlagContextResolver {

  @Override
  public Mono<FeatureFlagContext> resolve(ServerHttpRequest request) {
    return Mono.just(new FeatureFlagContext(UUID.randomUUID().toString()));
  }
}
