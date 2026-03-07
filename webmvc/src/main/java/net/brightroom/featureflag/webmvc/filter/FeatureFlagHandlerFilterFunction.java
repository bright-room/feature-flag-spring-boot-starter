package net.brightroom.featureflag.webmvc.filter;

import net.brightroom.featureflag.core.evaluation.AccessDecision;
import net.brightroom.featureflag.core.evaluation.EvaluationContext;
import net.brightroom.featureflag.core.evaluation.FeatureFlagEvaluationPipeline;
import net.brightroom.featureflag.core.exception.FeatureFlagAccessDeniedException;
import net.brightroom.featureflag.core.provider.ConditionProvider;
import net.brightroom.featureflag.core.provider.RolloutPercentageProvider;
import net.brightroom.featureflag.webmvc.condition.HttpServletConditionVariables;
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
 * <p>Use {@link #of(String, int)} to enable gradual rollout for functional endpoints with a
 * fallback rollout percentage.
 */
public class FeatureFlagHandlerFilterFunction {

  private final FeatureFlagEvaluationPipeline pipeline;
  private final AccessDeniedHandlerFilterResolution resolution;
  private final RolloutPercentageProvider rolloutPercentageProvider;
  private final ConditionProvider conditionProvider;
  private final FeatureFlagContextResolver contextResolver;

  /**
   * Creates a {@link HandlerFilterFunction} that guards the route with the specified feature flag.
   *
   * <p>Condition and rollout percentage are resolved from the configured providers.
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
   * and fallback condition expression.
   *
   * <p>The condition is resolved from the provider first; the {@code conditionFallback} is used
   * only when the provider returns no value for the feature.
   *
   * @param featureName the name of the feature flag to check; must not be null or blank
   * @param conditionFallback SpEL expression used as fallback when the provider has no condition
   *     configured; empty string means no condition
   * @return a {@link HandlerFilterFunction} that allows or denies access based on the feature flag
   *     and condition
   * @throws IllegalArgumentException if {@code featureName} is null or blank
   */
  public HandlerFilterFunction<ServerResponse, ServerResponse> of(
      String featureName, String conditionFallback) {
    return of(featureName, conditionFallback, 100);
  }

  /**
   * Creates a {@link HandlerFilterFunction} that guards the route with the specified feature flag
   * and fallback rollout percentage.
   *
   * <p>The rollout percentage is resolved from the provider first; the {@code rolloutFallback} is
   * used only when the provider returns no value for the feature.
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
   * fallback SpEL condition expression, and fallback rollout percentage.
   *
   * <p>The condition and rollout percentage are resolved from their respective providers first;
   * fallback values are used only when the providers return no value for the feature.
   *
   * <p>The evaluation order is: feature enabled check → schedule check → condition check → rollout
   * check.
   *
   * @param featureName the name of the feature flag to check; must not be null or blank
   * @param conditionFallback SpEL expression used as fallback when the provider has no condition
   *     configured; empty string means no condition
   * @param rolloutFallback the fallback rollout percentage (0–100) when no value is configured in
   *     the provider; 100 means fully enabled
   * @return a {@link HandlerFilterFunction} that allows or denies access based on the feature flag,
   *     condition, and rollout
   * @throws IllegalArgumentException if {@code featureName} is null or blank, or if {@code
   *     rolloutFallback} is not between 0 and 100
   */
  public HandlerFilterFunction<ServerResponse, ServerResponse> of(
      String featureName, String conditionFallback, int rolloutFallback) {
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
      String condition = conditionProvider.getCondition(featureName).orElse(conditionFallback);
      int rollout =
          rolloutPercentageProvider.getRolloutPercentage(featureName).orElse(rolloutFallback);
      EvaluationContext context =
          new EvaluationContext(
              featureName,
              condition,
              rollout,
              HttpServletConditionVariables.build(request.servletRequest()),
              () -> contextResolver.resolve(request.servletRequest()).orElse(null));
      AccessDecision decision = pipeline.evaluate(context);
      if (decision instanceof AccessDecision.Denied denied) {
        return resolution.resolve(
            request, new FeatureFlagAccessDeniedException(denied.featureName()));
      }
      return next.handle(request);
    };
  }

  /**
   * Creates a new {@link FeatureFlagHandlerFilterFunction}.
   *
   * @param pipeline the evaluation pipeline that performs all feature flag checks; must not be null
   * @param resolution the resolution strategy invoked when access is denied; must not be null
   * @param rolloutPercentageProvider the provider used to look up the rollout percentage per
   *     feature; must not be null
   * @param conditionProvider the provider used to look up the condition expression per feature;
   *     must not be null
   * @param contextResolver the resolver used to obtain the feature flag context from the request;
   *     must not be null
   */
  public FeatureFlagHandlerFilterFunction(
      FeatureFlagEvaluationPipeline pipeline,
      AccessDeniedHandlerFilterResolution resolution,
      RolloutPercentageProvider rolloutPercentageProvider,
      ConditionProvider conditionProvider,
      FeatureFlagContextResolver contextResolver) {
    this.pipeline = pipeline;
    this.resolution = resolution;
    this.rolloutPercentageProvider = rolloutPercentageProvider;
    this.conditionProvider = conditionProvider;
    this.contextResolver = contextResolver;
  }
}
