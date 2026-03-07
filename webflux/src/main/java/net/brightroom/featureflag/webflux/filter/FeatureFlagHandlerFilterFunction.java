package net.brightroom.featureflag.webflux.filter;

import java.time.Clock;
import net.brightroom.featureflag.core.condition.ReactiveFeatureFlagConditionEvaluator;
import net.brightroom.featureflag.core.context.FeatureFlagContext;
import net.brightroom.featureflag.core.exception.FeatureFlagAccessDeniedException;
import net.brightroom.featureflag.core.provider.ReactiveFeatureFlagProvider;
import net.brightroom.featureflag.core.provider.ReactiveRolloutPercentageProvider;
import net.brightroom.featureflag.core.provider.ReactiveScheduleProvider;
import net.brightroom.featureflag.webflux.condition.ServerHttpConditionVariables;
import net.brightroom.featureflag.webflux.context.ReactiveFeatureFlagContextResolver;
import net.brightroom.featureflag.webflux.resolution.handlerfilter.AccessDeniedHandlerFilterResolution;
import net.brightroom.featureflag.webflux.rollout.ReactiveRolloutStrategy;
import org.springframework.web.reactive.function.server.HandlerFilterFunction;
import org.springframework.web.reactive.function.server.HandlerFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
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
  private final ReactiveFeatureFlagConditionEvaluator conditionEvaluator;
  private final ReactiveScheduleProvider reactiveScheduleProvider;
  private final Clock clock;

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
   * <p>The evaluation order is: feature enabled check → condition check → rollout check.
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
    return (request, next) -> filter(request, next, featureName, condition, rolloutFallback);
  }

  private Mono<ServerResponse> filter(
      ServerRequest request,
      HandlerFunction<ServerResponse> next,
      String featureName,
      String condition,
      int rolloutFallback) {
    return reactiveFeatureFlagProvider
        .isFeatureEnabled(featureName)
        .defaultIfEmpty(false)
        .flatMap(
            enabled ->
                handleEnabled(request, next, featureName, condition, rolloutFallback, enabled));
  }

  private Mono<ServerResponse> handleEnabled(
      ServerRequest request,
      HandlerFunction<ServerResponse> next,
      String featureName,
      String condition,
      int rolloutFallback,
      boolean enabled) {
    if (!enabled) {
      return resolution.resolve(request, new FeatureFlagAccessDeniedException(featureName));
    }
    return reactiveScheduleProvider
        .getSchedule(featureName)
        .map(schedule -> schedule.isActive(clock.instant()))
        .defaultIfEmpty(true)
        .flatMap(
            active -> {
              if (!active) {
                return resolution.resolve(
                    request, new FeatureFlagAccessDeniedException(featureName));
              }
              return evaluateCondition(request, next, featureName, condition, rolloutFallback);
            });
  }

  private Mono<ServerResponse> evaluateCondition(
      ServerRequest request,
      HandlerFunction<ServerResponse> next,
      String featureName,
      String condition,
      int rolloutFallback) {
    Mono<Boolean> conditionMono;
    if (condition != null && !condition.isEmpty()) {
      conditionMono =
          conditionEvaluator.evaluate(
              condition, ServerHttpConditionVariables.build(request.exchange().getRequest()));
    } else {
      conditionMono = Mono.just(true);
    }
    return conditionMono.flatMap(
        passed -> handleConditionResult(request, next, featureName, rolloutFallback, passed));
  }

  private Mono<ServerResponse> handleConditionResult(
      ServerRequest request,
      HandlerFunction<ServerResponse> next,
      String featureName,
      int rolloutFallback,
      boolean passed) {
    if (!passed) {
      return resolution.resolve(request, new FeatureFlagAccessDeniedException(featureName));
    }
    return applyRollout(request, next, featureName, rolloutFallback);
  }

  private Mono<ServerResponse> applyRollout(
      ServerRequest request,
      HandlerFunction<ServerResponse> next,
      String featureName,
      int rolloutFallback) {
    return rolloutPercentageProvider
        .getRolloutPercentage(featureName)
        .defaultIfEmpty(rolloutFallback)
        .flatMap(rollout -> handleRollout(request, next, featureName, rollout));
  }

  private Mono<ServerResponse> handleRollout(
      ServerRequest request,
      HandlerFunction<ServerResponse> next,
      String featureName,
      int rollout) {
    if (rollout >= 100) {
      return next.handle(request);
    }
    return contextResolver
        .resolve(request.exchange().getRequest())
        .flatMap(ctx -> checkInRollout(request, next, featureName, ctx, rollout))
        .switchIfEmpty(Mono.defer(() -> next.handle(request)));
  }

  private Mono<ServerResponse> checkInRollout(
      ServerRequest request,
      HandlerFunction<ServerResponse> next,
      String featureName,
      FeatureFlagContext ctx,
      int rollout) {
    return rolloutStrategy
        .isInRollout(featureName, ctx, rollout)
        .flatMap(inRollout -> handleRolloutResult(request, next, featureName, inRollout));
  }

  private Mono<ServerResponse> handleRolloutResult(
      ServerRequest request,
      HandlerFunction<ServerResponse> next,
      String featureName,
      boolean inRollout) {
    if (!inRollout) {
      return resolution.resolve(request, new FeatureFlagAccessDeniedException(featureName));
    }
    return next.handle(request);
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
   * @param conditionEvaluator the reactive evaluator used to evaluate SpEL condition expressions
   * @param reactiveScheduleProvider the provider used to look up the schedule per feature
   * @param clock the clock used to obtain the current time for schedule evaluation
   */
  public FeatureFlagHandlerFilterFunction(
      ReactiveFeatureFlagProvider reactiveFeatureFlagProvider,
      AccessDeniedHandlerFilterResolution resolution,
      ReactiveRolloutStrategy rolloutStrategy,
      ReactiveFeatureFlagContextResolver contextResolver,
      ReactiveRolloutPercentageProvider rolloutPercentageProvider,
      ReactiveFeatureFlagConditionEvaluator conditionEvaluator,
      ReactiveScheduleProvider reactiveScheduleProvider,
      Clock clock) {
    this.reactiveFeatureFlagProvider = reactiveFeatureFlagProvider;
    this.resolution = resolution;
    this.rolloutStrategy = rolloutStrategy;
    this.contextResolver = contextResolver;
    this.rolloutPercentageProvider = rolloutPercentageProvider;
    this.conditionEvaluator = conditionEvaluator;
    this.reactiveScheduleProvider = reactiveScheduleProvider;
    this.clock = clock;
  }
}
