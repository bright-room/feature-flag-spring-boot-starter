package net.brightroom.featureflag.webmvc.context;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import java.util.UUID;
import net.brightroom.featureflag.core.context.FeatureFlagContext;

/**
 * Default {@link FeatureFlagContextResolver} that generates a random UUID per request.
 *
 * <p>This provides non-sticky (per-request probabilistic) rollout behavior. Each request
 * independently has a rollout-percentage chance of being included.
 *
 * <p>This class is package-private. Users interact with {@link FeatureFlagContextResolver} only.
 */
class RandomFeatureFlagContextResolver implements FeatureFlagContextResolver {

  @Override
  public Optional<FeatureFlagContext> resolve(HttpServletRequest request) {
    return Optional.of(new FeatureFlagContext(UUID.randomUUID().toString()));
  }
}
