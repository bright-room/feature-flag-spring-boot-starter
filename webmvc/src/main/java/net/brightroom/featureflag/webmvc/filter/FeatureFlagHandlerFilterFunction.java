package net.brightroom.featureflag.webmvc.filter;

import java.util.Optional;
import net.brightroom.featureflag.core.context.FeatureFlagContext;
import net.brightroom.featureflag.core.exception.FeatureFlagAccessDeniedException;
import net.brightroom.featureflag.core.provider.FeatureFlagProvider;
import net.brightroom.featureflag.core.provider.RolloutPercentageProvider;
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
  private final RolloutPercentageProvider rolloutPercentageProvider;

  /**
   * Creates a {@link HandlerFilterFunction} that guards the route with the specified feature flag.
   *
   * @param featureName the name of the feature flag to check; must not be null or blank
   * @return a {@link HandlerFilterFunction} that allows or denies access based on the feature flag
   * @throws IllegalArgumentException if {@code featureName} is null or blank
   */
  public HandlerFilterFunction<ServerResponse, ServerResponse> of(String featureName) {
    return of(featureName, 100);
  }

  /**
   * Creates a {@link HandlerFilterFunction} that guards the route with the specified feature flag
   * and rollout percentage.
   *
   * <p>The rollout percentage is resolved from the {@link RolloutPercentageProvider} first. If no
   * rollout percentage is configured in the provider, the {@code rolloutFallback} argument is used
   * as a fallback.
   *
   * @param featureName the name of the feature flag to check; must not be null or blank
   * @param rolloutFallback the fallback rollout percentage (0–100) when no value is configured in
   *     the provider; 100 means fully enabled
   * @return a {@link HandlerFilterFunction} that allows or denies access based on the feature flag
   *     and rollout
   * @throws IllegalArgumentException if {@code featureName} is null or blank, or if {@code
   *     rolloutFallback} is not between 0 and 100
   */
  public HandlerFilterFunction<ServerResponse, ServerResponse> of(
      String featureName, int rolloutFallback) {
    if (featureName == null || featureName.isBlank()) {
      throw new IllegalArgumentException(
          "featureName must not be null or blank. "
              + "A blank value causes fail-open behavior and allows access unconditionally.");
    }
    if (rolloutFallback < 0 || rolloutFallback > 100) {
      throw new IllegalArgumentException(
          "rollout must be between 0 and 100, but was: " + rolloutFallback);
    }
    return (request, next) -> {
      if (!featureFlagProvider.isFeatureEnabled(featureName)) {
        return resolution.resolve(request, new FeatureFlagAccessDeniedException(featureName));
      }
      int rollout =
          rolloutPercentageProvider.getRolloutPercentage(featureName).orElse(rolloutFallback);
      if (rollout < 100) {
        Optional<FeatureFlagContext> ctx = contextResolver.resolve(request.servletRequest());
        if (ctx.isPresent() && !rolloutStrategy.isInRollout(featureName, ctx.get(), rollout)) {
          return resolution.resolve(request, new FeatureFlagAccessDeniedException(featureName));
        }
      }
      return next.handle(request);
    };
  }

  /**
   * Creates a new {@link FeatureFlagHandlerFilterFunction}.
   *
   * @param featureFlagProvider the provider used to check whether a feature flag is enabled; must
   *     not be null
   * @param resolution the resolution strategy invoked when access is denied; must not be null
   * @param rolloutStrategy the strategy used to determine rollout bucket membership; must not be
   *     null
   * @param contextResolver the resolver used to obtain the feature flag context from the request;
   *     must not be null
   * @param rolloutPercentageProvider the provider used to look up the rollout percentage per
   *     feature; must not be null
   */
  public FeatureFlagHandlerFilterFunction(
      FeatureFlagProvider featureFlagProvider,
      AccessDeniedHandlerFilterResolution resolution,
      RolloutStrategy rolloutStrategy,
      FeatureFlagContextResolver contextResolver,
      RolloutPercentageProvider rolloutPercentageProvider) {
    this.featureFlagProvider = featureFlagProvider;
    this.resolution = resolution;
    this.rolloutStrategy = rolloutStrategy;
    this.contextResolver = contextResolver;
    this.rolloutPercentageProvider = rolloutPercentageProvider;
  }
}
