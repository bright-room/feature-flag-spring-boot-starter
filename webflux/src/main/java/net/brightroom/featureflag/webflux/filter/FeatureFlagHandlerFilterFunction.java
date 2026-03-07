package net.brightroom.featureflag.webflux.filter;

import net.brightroom.featureflag.core.evaluation.AccessDecision;
import net.brightroom.featureflag.core.evaluation.EvaluationContext;
import net.brightroom.featureflag.core.evaluation.ReactiveFeatureFlagEvaluationPipeline;
import net.brightroom.featureflag.core.exception.FeatureFlagAccessDeniedException;
import net.brightroom.featureflag.core.provider.ReactiveRolloutPercentageProvider;
import net.brightroom.featureflag.webflux.condition.ServerHttpConditionVariables;
import net.brightroom.featureflag.webflux.context.ReactiveFeatureFlagContextResolver;
import net.brightroom.featureflag.webflux.resolution.handlerfilter.AccessDeniedHandlerFilterResolution;
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

  private final ReactiveFeatureFlagEvaluationPipeline pipeline;
  private final AccessDeniedHandlerFilterResolution resolution;
  private final ReactiveRolloutPercentageProvider rolloutPercentageProvider;
  private final ReactiveFeatureFlagContextResolver contextResolver;

  /**
   * Creates a {@link HandlerFilterFunction} that guards the route with the specified feature flag.
   *
   * @param featureName the name of the feature flag to check; must not be null or blank
   * @return a {@link HandlerFilterFunction} that allows or denies access based on the feature flag
   * @throws IllegalArgumentException if {@code featureName} is null or blank
   */
  public HandlerFilterFunction<ServerResponse, ServerResponse> of(String featureName) {
    return of(featureName, "", 100);
  }

  /**
   * Creates a {@link HandlerFilterFunction} that guards the route with the specified feature flag
   * and SpEL condition expression.
   *
   * @param featureName the name of the feature flag to check; must not be null or blank
   * @param condition SpEL expression evaluated against request context; empty string means no
   *     condition
   * @return a {@link HandlerFilterFunction} that allows or denies access based on the feature flag
   *     and condition
   * @throws IllegalArgumentException if {@code featureName} is null or blank
   */
  public HandlerFilterFunction<ServerResponse, ServerResponse> of(
      String featureName, String condition) {
    return of(featureName, condition, 100);
  }

  /**
   * Creates a {@link HandlerFilterFunction} that guards the route with the specified feature flag
   * and rollout percentage.
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
    return of(featureName, "", rolloutFallback);
  }

  /**
   * Creates a {@link HandlerFilterFunction} that guards the route with the specified feature flag,
   * SpEL condition expression, and rollout percentage.
   *
   * <p>The evaluation order is: feature enabled check → schedule check → condition check → rollout
   * check.
   *
   * @param featureName the name of the feature flag to check; must not be null or blank
   * @param condition SpEL expression evaluated against request context; empty string means no
   *     condition
   * @param rolloutFallback the fallback rollout percentage (0–100) when no value is configured in
   *     the provider; 100 means fully enabled
   * @return a {@link HandlerFilterFunction} that allows or denies access based on the feature flag,
   *     condition, and rollout
   * @throws IllegalArgumentException if {@code featureName} is null or blank, or if {@code
   *     rolloutFallback} is not between 0 and 100
   */
  public HandlerFilterFunction<ServerResponse, ServerResponse> of(
      String featureName, String condition, int rolloutFallback) {
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
        Mono.zip(
                rolloutPercentageProvider
                    .getRolloutPercentage(featureName)
                    .defaultIfEmpty(rolloutFallback),
                contextResolver
                    .resolve(request.exchange().getRequest())
                    .map(java.util.Optional::of)
                    .defaultIfEmpty(java.util.Optional.empty()))
            .flatMap(
                tuple -> {
                  EvaluationContext evalCtx =
                      new EvaluationContext(
                          featureName,
                          condition,
                          tuple.getT1(),
                          ServerHttpConditionVariables.build(request.exchange().getRequest()),
                          tuple.getT2().orElse(null));
                  return pipeline.evaluate(evalCtx);
                })
            .flatMap(
                decision -> {
                  if (decision instanceof AccessDecision.Denied denied) {
                    return resolution.resolve(
                        request, new FeatureFlagAccessDeniedException(denied.featureName()));
                  }
                  return next.handle(request);
                });
  }

  /**
   * Creates a new {@code FeatureFlagHandlerFilterFunction}.
   *
   * @param pipeline the reactive evaluation pipeline that performs all feature flag checks; must
   *     not be null
   * @param resolution the resolution used to build the denied response for functional endpoints;
   *     must not be null
   * @param rolloutPercentageProvider the provider used to look up the rollout percentage per
   *     feature; must not be null
   * @param contextResolver the resolver used to extract context from the current request; must not
   *     be null
   */
  public FeatureFlagHandlerFilterFunction(
      ReactiveFeatureFlagEvaluationPipeline pipeline,
      AccessDeniedHandlerFilterResolution resolution,
      ReactiveRolloutPercentageProvider rolloutPercentageProvider,
      ReactiveFeatureFlagContextResolver contextResolver) {
    this.pipeline = pipeline;
    this.resolution = resolution;
    this.rolloutPercentageProvider = rolloutPercentageProvider;
    this.contextResolver = contextResolver;
  }
}
