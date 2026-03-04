package net.brightroom.featureflag.webmvc.filter;

import java.util.Optional;
import net.brightroom.featureflag.core.context.FeatureFlagContext;
import net.brightroom.featureflag.core.exception.FeatureFlagAccessDeniedException;
import net.brightroom.featureflag.core.provider.FeatureFlagProvider;
import net.brightroom.featureflag.core.rollout.RolloutStrategy;
import net.brightroom.featureflag.webmvc.context.FeatureFlagContextResolver;
import net.brightroom.featureflag.webmvc.resolution.handlerfilter.AccessDeniedHandlerFilterResolution;
import org.springframework.web.servlet.function.HandlerFilterFunction;
import org.springframework.web.servlet.function.ServerResponse;

/**
 * A factory for {@link HandlerFilterFunction} that applies feature flag access control to
 * Functional Endpoints.
 *
 * <p>Use {@link #of(String)} to create a {@link HandlerFilterFunction} for a specific feature name
 * and apply it to a {@link org.springframework.web.servlet.function.RouterFunction}:
 *
 * <pre>{@code
 * @Bean
 * RouterFunction<ServerResponse> routes(FeatureFlagHandlerFilterFunction featureFlagFilter) {
 *     return route()
 *         .GET("/api/feature", handler::handle)
 *         .filter(featureFlagFilter.of("my-feature"))
 *         .build();
 * }
 * }</pre>
 *
 * <p>When the feature is disabled, the filter delegates to {@link
 * AccessDeniedHandlerFilterResolution} to build the denied response without invoking the handler.
 * The default response format follows {@code feature-flags.response.type} configuration, and can be
 * customized by providing a custom {@link AccessDeniedHandlerFilterResolution} bean.
 *
 * <p>Use {@link #of(String, int)} to enable gradual rollout for functional endpoints.
 */
public class FeatureFlagHandlerFilterFunction {

  private final FeatureFlagProvider featureFlagProvider;
  private final AccessDeniedHandlerFilterResolution resolution;
  private final RolloutStrategy rolloutStrategy;
  private final FeatureFlagContextResolver contextResolver;

  /**
   * Creates a {@link HandlerFilterFunction} that guards the route with the specified feature flag.
   *
   * @param featureName the name of the feature flag to check; must not be null or empty
   * @return a {@link HandlerFilterFunction} that allows or denies access based on the feature flag
   * @throws IllegalArgumentException if {@code featureName} is null or empty
   */
  public HandlerFilterFunction<ServerResponse, ServerResponse> of(String featureName) {
    return of(featureName, 100);
  }

  /**
   * Creates a {@link HandlerFilterFunction} that guards the route with the specified feature flag
   * and rollout percentage.
   *
   * @param featureName the name of the feature flag to check; must not be null or empty
   * @param rollout the rollout percentage (0–100); 100 means fully enabled
   * @return a {@link HandlerFilterFunction} that allows or denies access based on the feature flag
   *     and rollout
   * @throws IllegalArgumentException if {@code featureName} is null or empty, or if {@code rollout}
   *     is not between 0 and 100
   */
  public HandlerFilterFunction<ServerResponse, ServerResponse> of(String featureName, int rollout) {
    if (featureName == null || featureName.isEmpty()) {
      throw new IllegalArgumentException(
          "featureName must not be null or empty. "
              + "An empty value causes fail-open behavior and allows access unconditionally.");
    }
    if (rollout < 0 || rollout > 100) {
      throw new IllegalArgumentException("rollout must be between 0 and 100, but was: " + rollout);
    }
    return (request, next) -> {
      if (!featureFlagProvider.isFeatureEnabled(featureName)) {
        return resolution.resolve(request, new FeatureFlagAccessDeniedException(featureName));
      }
      if (rollout < 100) {
        Optional<FeatureFlagContext> ctx = contextResolver.resolve(request.servletRequest());
        if (ctx.isPresent() && !rolloutStrategy.isInRollout(featureName, ctx.get(), rollout)) {
          return resolution.resolve(request, new FeatureFlagAccessDeniedException(featureName));
        }
      }
      return next.handle(request);
    };
  }

  public FeatureFlagHandlerFilterFunction(
      FeatureFlagProvider featureFlagProvider,
      AccessDeniedHandlerFilterResolution resolution,
      RolloutStrategy rolloutStrategy,
      FeatureFlagContextResolver contextResolver) {
    this.featureFlagProvider = featureFlagProvider;
    this.resolution = resolution;
    this.rolloutStrategy = rolloutStrategy;
    this.contextResolver = contextResolver;
  }
}
