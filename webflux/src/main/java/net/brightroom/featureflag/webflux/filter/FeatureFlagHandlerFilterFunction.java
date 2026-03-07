package net.brightroom.featureflag.webflux.filter;

import net.brightroom.featureflag.core.exception.FeatureFlagAccessDeniedException;
import net.brightroom.featureflag.core.provider.ReactiveFeatureFlagProvider;
import net.brightroom.featureflag.core.provider.ReactiveRolloutPercentageProvider;
import net.brightroom.featureflag.webflux.context.ReactiveFeatureFlagContextResolver;
import net.brightroom.featureflag.webflux.resolution.handlerfilter.AccessDeniedHandlerFilterResolution;
import net.brightroom.featureflag.webflux.rollout.ReactiveRolloutStrategy;
import org.springframework.web.reactive.function.server.HandlerFilterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

/**
 * A factory for {@link HandlerFilterFunction} that applies feature flag access control to
 * Functional Endpoints.
 *
 * <p>Use {@link #of(String)} to create a {@link HandlerFilterFunction} for a specific feature name
 * and apply it to a {@link org.springframework.web.reactive.function.server.RouterFunction}:
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

  private final ReactiveFeatureFlagProvider reactiveFeatureFlagProvider;
  private final AccessDeniedHandlerFilterResolution resolution;
  private final ReactiveRolloutStrategy rolloutStrategy;
  private final ReactiveFeatureFlagContextResolver contextResolver;
  private final ReactiveRolloutPercentageProvider rolloutPercentageProvider;

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
   * <p>The rollout percentage is resolved from the {@link ReactiveRolloutPercentageProvider} first.
   * If no rollout percentage is configured in the provider, the {@code rolloutFallback} argument is
   * used as a fallback.
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
    return (request, next) ->
        reactiveFeatureFlagProvider
            .isFeatureEnabled(featureName)
            .defaultIfEmpty(false)
            .flatMap(
                enabled -> {
                  if (!enabled) {
                    return resolution.resolve(
                        request, new FeatureFlagAccessDeniedException(featureName));
                  }
                  return rolloutPercentageProvider
                      .getRolloutPercentage(featureName)
                      .defaultIfEmpty(rolloutFallback)
                      .flatMap(
                          rollout -> {
                            if (rollout < 100) {
                              return contextResolver
                                  .resolve(request.exchange().getRequest())
                                  .flatMap(
                                      ctx ->
                                          rolloutStrategy
                                              .isInRollout(featureName, ctx, rollout)
                                              .flatMap(
                                                  inRollout -> {
                                                    if (!inRollout) {
                                                      return resolution.resolve(
                                                          request,
                                                          new FeatureFlagAccessDeniedException(
                                                              featureName));
                                                    }
                                                    return next.handle(request);
                                                  }))
                                  .switchIfEmpty(Mono.defer(() -> next.handle(request)));
                            }
                            return next.handle(request);
                          });
                });
  }

  /**
   * Creates a new {@code FeatureFlagHandlerFilterFunction}.
   *
   * @param reactiveFeatureFlagProvider the provider used to check whether a feature flag is enabled
   * @param resolution the resolution used to build the denied response for functional endpoints
   * @param rolloutStrategy the strategy used to determine rollout bucket membership
   * @param contextResolver the resolver used to extract context from the current request
   * @param rolloutPercentageProvider the provider used to look up the rollout percentage per
   *     feature
   */
  public FeatureFlagHandlerFilterFunction(
      ReactiveFeatureFlagProvider reactiveFeatureFlagProvider,
      AccessDeniedHandlerFilterResolution resolution,
      ReactiveRolloutStrategy rolloutStrategy,
      ReactiveFeatureFlagContextResolver contextResolver,
      ReactiveRolloutPercentageProvider rolloutPercentageProvider) {
    this.reactiveFeatureFlagProvider = reactiveFeatureFlagProvider;
    this.resolution = resolution;
    this.rolloutStrategy = rolloutStrategy;
    this.contextResolver = contextResolver;
    this.rolloutPercentageProvider = rolloutPercentageProvider;
  }
}
