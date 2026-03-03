package net.brightroom.featureflag.webmvc.context;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import net.brightroom.featureflag.core.context.FeatureFlagContext;

/**
 * Resolves the feature flag context from the current HTTP request.
 *
 * <p>The context is used for rollout percentage checks. The default implementation ({@code
 * RandomFeatureFlagContextResolver}) generates a random UUID per request, providing non-sticky
 * (per-request probabilistic) rollout behavior.
 *
 * <p>To achieve sticky rollout (same user always gets the same result), implement this interface
 * and register it as a {@code @Bean}. The custom bean will replace the default due to
 * {@code @ConditionalOnMissingBean}.
 *
 * <p>Return {@link Optional#empty()} if the identifier cannot be resolved. In that case, the
 * rollout check is skipped and the feature is treated as fully enabled (fail-open).
 */
public interface FeatureFlagContextResolver {

  /**
   * Resolves the feature flag context from the current request.
   *
   * @param request the current HTTP request
   * @return the resolved context, or {@link Optional#empty()} to skip the rollout check
   */
  Optional<FeatureFlagContext> resolve(HttpServletRequest request);
}
