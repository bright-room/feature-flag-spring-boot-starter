package net.brightroom.featureflag.webflux.aspect;

import java.lang.reflect.Method;
import net.brightroom.featureflag.core.annotation.FeatureFlag;
import net.brightroom.featureflag.core.condition.ReactiveFeatureFlagConditionEvaluator;
import net.brightroom.featureflag.core.exception.FeatureFlagAccessDeniedException;
import net.brightroom.featureflag.core.provider.ReactiveFeatureFlagProvider;
import net.brightroom.featureflag.core.provider.ReactiveRolloutPercentageProvider;
import net.brightroom.featureflag.webflux.condition.ServerHttpConditionVariables;
import net.brightroom.featureflag.webflux.context.ReactiveFeatureFlagContextResolver;
import net.brightroom.featureflag.webflux.rollout.ReactiveRolloutStrategy;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.aop.support.AopUtils;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * AOP aspect that enforces feature flag access control on Spring WebFlux controller methods.
 *
 * <p>Intercepts methods and classes annotated with {@link
 * net.brightroom.featureflag.core.annotation.FeatureFlag} via an {@code @Around} advice. If the
 * referenced feature flag is disabled, a {@link
 * net.brightroom.featureflag.core.exception.FeatureFlagAccessDeniedException} is emitted into the
 * reactive pipeline. The method-level annotation takes priority over the class-level annotation.
 *
 * <p>Only reactive return types ({@link reactor.core.publisher.Mono} and {@link
 * reactor.core.publisher.Flux}) are supported; non-reactive return types throw {@link
 * IllegalStateException}.
 */
@Aspect
public class FeatureFlagAspect {

  private final ReactiveFeatureFlagProvider reactiveFeatureFlagProvider;
  private final ReactiveRolloutStrategy rolloutStrategy;
  private final ReactiveFeatureFlagContextResolver contextResolver;
  private final ReactiveRolloutPercentageProvider rolloutPercentageProvider;
  private final ReactiveFeatureFlagConditionEvaluator conditionEvaluator;

  /**
   * Around advice that checks the feature flag before proceeding with the annotated method.
   *
   * <p>Applies to methods and classes annotated with {@link
   * net.brightroom.featureflag.core.annotation.FeatureFlag}. If the feature is disabled, a {@link
   * net.brightroom.featureflag.core.exception.FeatureFlagAccessDeniedException} is returned in the
   * reactive pipeline. Rollout percentage is also evaluated when configured.
   *
   * @param joinPoint the proceeding join point of the intercepted method
   * @return the result of the intercepted method, or an error signal if access is denied
   * @throws Throwable if the underlying method throws an exception
   */
  @Around(
      "@within(net.brightroom.featureflag.core.annotation.FeatureFlag) || "
          + "@annotation(net.brightroom.featureflag.core.annotation.FeatureFlag)")
  public Object checkFeatureFlag(ProceedingJoinPoint joinPoint) throws Throwable {
    FeatureFlag annotation = resolveAnnotation(joinPoint);
    if (annotation == null) {
      return joinPoint.proceed();
    }

    validateAnnotation(annotation);

    String featureName = annotation.value();
    String condition = annotation.condition();
    int annotationRollout = annotation.rollout();
    Mono<Boolean> enabledMono =
        reactiveFeatureFlagProvider.isFeatureEnabled(featureName).defaultIfEmpty(false);
    Mono<Integer> rolloutMono =
        rolloutPercentageProvider
            .getRolloutPercentage(featureName)
            .defaultIfEmpty(annotationRollout);

    Class<?> returnType = ((MethodSignature) joinPoint.getSignature()).getReturnType();

    if (Mono.class.isAssignableFrom(returnType)) {
      return enabledMono.flatMap(
          enabled -> {
            if (!enabled) {
              return Mono.error(new FeatureFlagAccessDeniedException(featureName));
            }
            if (!condition.isEmpty()) {
              return Mono.deferContextual(
                  ctx -> {
                    ServerWebExchange exchange = ctx.get(ServerWebExchange.class);
                    return conditionEvaluator
                        .evaluate(
                            condition, ServerHttpConditionVariables.build(exchange.getRequest()))
                        .flatMap(
                            passed -> {
                              if (!passed) {
                                return Mono.error(
                                    new FeatureFlagAccessDeniedException(featureName));
                              }
                              return proceedMonoWithRollout(
                                  joinPoint, featureName, rolloutMono, exchange);
                            });
                  });
            }
            return rolloutMono.flatMap(
                rollout -> {
                  if (rollout < 100) {
                    return Mono.deferContextual(
                        ctx -> {
                          ServerWebExchange exchange = ctx.get(ServerWebExchange.class);
                          return shouldProceed(featureName, exchange, rollout)
                              .flatMap(
                                  proceed -> {
                                    if (!proceed) {
                                      return Mono.error(
                                          new FeatureFlagAccessDeniedException(featureName));
                                    }
                                    return proceedAsMono(joinPoint);
                                  });
                        });
                  }
                  return proceedAsMono(joinPoint);
                });
          });
    }

    if (Flux.class.isAssignableFrom(returnType)) {
      return enabledMono.flatMapMany(
          enabled -> {
            if (!enabled) {
              return Flux.error(new FeatureFlagAccessDeniedException(featureName));
            }
            if (!condition.isEmpty()) {
              return Flux.deferContextual(
                  ctx -> {
                    ServerWebExchange exchange = ctx.get(ServerWebExchange.class);
                    return conditionEvaluator
                        .evaluate(
                            condition, ServerHttpConditionVariables.build(exchange.getRequest()))
                        .flatMapMany(
                            passed -> {
                              if (!passed) {
                                return Flux.error(
                                    new FeatureFlagAccessDeniedException(featureName));
                              }
                              return proceedFluxWithRollout(
                                  joinPoint, featureName, rolloutMono, exchange);
                            });
                  });
            }
            return rolloutMono.flatMapMany(
                rollout -> {
                  if (rollout < 100) {
                    return Flux.deferContextual(
                        ctx -> {
                          ServerWebExchange exchange = ctx.get(ServerWebExchange.class);
                          return shouldProceed(featureName, exchange, rollout)
                              .flatMapMany(
                                  proceed -> {
                                    if (!proceed) {
                                      return Flux.error(
                                          new FeatureFlagAccessDeniedException(featureName));
                                    }
                                    return proceedAsFlux(joinPoint);
                                  });
                        });
                  }
                  return proceedAsFlux(joinPoint);
                });
          });
    }

    // Non-reactive return type: not supported in WebFlux
    throw new IllegalStateException(
        "@FeatureFlag on WebFlux controller method '"
            + ((MethodSignature) joinPoint.getSignature()).getMethod().getName()
            + "' requires a reactive return type (Mono or Flux). "
            + "Non-reactive return types are not supported.");
  }

  private Mono<Object> proceedMonoWithRollout(
      ProceedingJoinPoint joinPoint,
      String featureName,
      Mono<Integer> rolloutMono,
      ServerWebExchange exchange) {
    return rolloutMono.flatMap(
        rollout -> {
          if (rollout < 100) {
            return shouldProceed(featureName, exchange, rollout)
                .flatMap(
                    proceed -> {
                      if (!proceed) {
                        return Mono.error(new FeatureFlagAccessDeniedException(featureName));
                      }
                      return proceedAsMono(joinPoint);
                    });
          }
          return proceedAsMono(joinPoint);
        });
  }

  private Flux<Object> proceedFluxWithRollout(
      ProceedingJoinPoint joinPoint,
      String featureName,
      Mono<Integer> rolloutMono,
      ServerWebExchange exchange) {
    return rolloutMono.flatMapMany(
        rollout -> {
          if (rollout < 100) {
            return shouldProceed(featureName, exchange, rollout)
                .flatMapMany(
                    proceed -> {
                      if (!proceed) {
                        return Flux.error(new FeatureFlagAccessDeniedException(featureName));
                      }
                      return proceedAsFlux(joinPoint);
                    });
          }
          return proceedAsFlux(joinPoint);
        });
  }

  /**
   * Resolves whether the request should proceed through the rollout check.
   *
   * <p>Returns {@code true} (proceed) when the context is empty (fail-open), or when the context is
   * within the rollout bucket. Returns {@code false} when the context is outside the rollout
   * bucket.
   */
  private Mono<Boolean> shouldProceed(String featureName, ServerWebExchange exchange, int rollout) {
    return contextResolver
        .resolve(exchange.getRequest())
        .flatMap(context -> rolloutStrategy.isInRollout(featureName, context, rollout))
        .defaultIfEmpty(true);
  }

  @SuppressWarnings("unchecked")
  private Mono<Object> proceedAsMono(ProceedingJoinPoint joinPoint) {
    try {
      return (Mono<Object>) joinPoint.proceed();
    } catch (Throwable t) {
      return Mono.error(t);
    }
  }

  @SuppressWarnings("unchecked")
  private Flux<Object> proceedAsFlux(ProceedingJoinPoint joinPoint) {
    try {
      return (Flux<Object>) joinPoint.proceed();
    } catch (Throwable t) {
      return Flux.error(t);
    }
  }

  private FeatureFlag resolveAnnotation(ProceedingJoinPoint joinPoint) {
    MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
    Method method =
        AopUtils.getMostSpecificMethod(
            methodSignature.getMethod(), joinPoint.getTarget().getClass());
    FeatureFlag methodAnnotation = AnnotationUtils.findAnnotation(method, FeatureFlag.class);
    if (methodAnnotation != null) {
      return methodAnnotation;
    }
    return AnnotationUtils.findAnnotation(joinPoint.getTarget().getClass(), FeatureFlag.class);
  }

  private void validateAnnotation(FeatureFlag annotation) {
    if (annotation.value().isEmpty()) {
      throw new IllegalStateException(
          "@FeatureFlag must specify a non-empty value. "
              + "An empty value causes fail-open behavior and allows access unconditionally.");
    }
    if (annotation.rollout() < 0 || annotation.rollout() > 100) {
      throw new IllegalStateException(
          "@FeatureFlag rollout must be between 0 and 100, but was: " + annotation.rollout());
    }
  }

  /**
   * Creates a new {@code FeatureFlagAspect}.
   *
   * @param reactiveFeatureFlagProvider the provider used to check whether a feature flag is enabled
   * @param rolloutStrategy the strategy used to determine rollout bucket membership
   * @param contextResolver the resolver used to extract context from the current request
   * @param rolloutPercentageProvider the provider used to look up the rollout percentage per
   *     feature
   * @param conditionEvaluator the reactive evaluator used to evaluate SpEL condition expressions
   */
  public FeatureFlagAspect(
      ReactiveFeatureFlagProvider reactiveFeatureFlagProvider,
      ReactiveRolloutStrategy rolloutStrategy,
      ReactiveFeatureFlagContextResolver contextResolver,
      ReactiveRolloutPercentageProvider rolloutPercentageProvider,
      ReactiveFeatureFlagConditionEvaluator conditionEvaluator) {
    this.reactiveFeatureFlagProvider = reactiveFeatureFlagProvider;
    this.rolloutStrategy = rolloutStrategy;
    this.contextResolver = contextResolver;
    this.rolloutPercentageProvider = rolloutPercentageProvider;
    this.conditionEvaluator = conditionEvaluator;
  }
}
