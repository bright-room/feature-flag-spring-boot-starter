package net.brightroom.featureflag.webflux.aspect;

import java.lang.reflect.Method;
import net.brightroom.featureflag.core.annotation.FeatureFlag;
import net.brightroom.featureflag.core.evaluation.AccessDecision;
import net.brightroom.featureflag.core.evaluation.EvaluationContext;
import net.brightroom.featureflag.core.evaluation.ReactiveFeatureFlagEvaluationPipeline;
import net.brightroom.featureflag.core.exception.FeatureFlagAccessDeniedException;
import net.brightroom.featureflag.core.provider.ReactiveRolloutPercentageProvider;
import net.brightroom.featureflag.webflux.condition.ServerHttpConditionVariables;
import net.brightroom.featureflag.webflux.context.ReactiveFeatureFlagContextResolver;
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

  private final ReactiveFeatureFlagEvaluationPipeline pipeline;
  private final ReactiveFeatureFlagContextResolver contextResolver;
  private final ReactiveRolloutPercentageProvider rolloutPercentageProvider;

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

    Class<?> returnType = ((MethodSignature) joinPoint.getSignature()).getReturnType();

    Mono<AccessDecision> decisionMono =
        Mono.deferContextual(
            ctx -> {
              ServerWebExchange exchange = ctx.get(ServerWebExchange.class);
              return Mono.zip(
                      rolloutPercentageProvider
                          .getRolloutPercentage(featureName)
                          .defaultIfEmpty(annotationRollout),
                      contextResolver
                          .resolve(exchange.getRequest())
                          .map(java.util.Optional::of)
                          .defaultIfEmpty(java.util.Optional.empty()))
                  .flatMap(
                      tuple -> {
                        EvaluationContext evalCtx =
                            new EvaluationContext(
                                featureName,
                                condition,
                                tuple.getT1(),
                                ServerHttpConditionVariables.build(exchange.getRequest()),
                                tuple.getT2().orElse(null));
                        return pipeline.evaluate(evalCtx);
                      });
            });

    if (Mono.class.isAssignableFrom(returnType)) {
      return decisionMono.flatMap(
          decision -> {
            if (decision instanceof AccessDecision.Denied denied) {
              return Mono.error(new FeatureFlagAccessDeniedException(denied.featureName()));
            }
            return proceedAsMono(joinPoint);
          });
    }

    if (Flux.class.isAssignableFrom(returnType)) {
      return decisionMono.flatMapMany(
          decision -> {
            if (decision instanceof AccessDecision.Denied denied) {
              return Flux.error(new FeatureFlagAccessDeniedException(denied.featureName()));
            }
            return proceedAsFlux(joinPoint);
          });
    }

    throw new IllegalStateException(
        "@FeatureFlag on WebFlux controller method '"
            + ((MethodSignature) joinPoint.getSignature()).getMethod().getName()
            + "' requires a reactive return type (Mono or Flux). "
            + "Non-reactive return types are not supported.");
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
   * @param pipeline the reactive evaluation pipeline that performs all feature flag checks; must
   *     not be null
   * @param contextResolver the resolver used to extract context from the current request; must not
   *     be null
   * @param rolloutPercentageProvider the provider used to look up the rollout percentage per
   *     feature; must not be null
   */
  public FeatureFlagAspect(
      ReactiveFeatureFlagEvaluationPipeline pipeline,
      ReactiveFeatureFlagContextResolver contextResolver,
      ReactiveRolloutPercentageProvider rolloutPercentageProvider) {
    this.pipeline = pipeline;
    this.contextResolver = contextResolver;
    this.rolloutPercentageProvider = rolloutPercentageProvider;
  }
}
