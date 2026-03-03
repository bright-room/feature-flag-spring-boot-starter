package net.brightroom.featureflag.webflux.aspect;

import java.lang.reflect.Method;
import net.brightroom.featureflag.core.annotation.FeatureFlag;
import net.brightroom.featureflag.core.exception.FeatureFlagAccessDeniedException;
import net.brightroom.featureflag.core.rollout.RolloutStrategy;
import net.brightroom.featureflag.webflux.context.ReactiveFeatureFlagContextResolver;
import net.brightroom.featureflag.webflux.provider.ReactiveFeatureFlagProvider;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.aop.support.AopUtils;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Aspect
public class FeatureFlagAspect {

  private final ReactiveFeatureFlagProvider reactiveFeatureFlagProvider;
  private final RolloutStrategy rolloutStrategy;
  private final ReactiveFeatureFlagContextResolver contextResolver;

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
    int rollout = annotation.rollout();
    Mono<Boolean> enabledMono =
        reactiveFeatureFlagProvider.isFeatureEnabled(featureName).defaultIfEmpty(false);

    Class<?> returnType = ((MethodSignature) joinPoint.getSignature()).getReturnType();

    if (Mono.class.isAssignableFrom(returnType)) {
      return enabledMono.flatMap(
          enabled -> {
            if (!enabled) {
              return Mono.error(new FeatureFlagAccessDeniedException(featureName));
            }
            if (rollout < 100) {
              return Mono.deferContextual(
                  ctx -> {
                    ServerWebExchange exchange = ctx.get(ServerWebExchange.class);
                    Mono<Object> proceedMono = proceedAsMono(joinPoint);
                    return contextResolver
                        .resolve(exchange.getRequest())
                        .<Object>flatMap(
                            context -> {
                              if (!rolloutStrategy.isInRollout(featureName, context, rollout)) {
                                return Mono.error(
                                    new FeatureFlagAccessDeniedException(featureName));
                              }
                              return proceedMono;
                            })
                        .switchIfEmpty(proceedMono);
                  });
            }
            return proceedAsMono(joinPoint);
          });
    }

    if (Flux.class.isAssignableFrom(returnType)) {
      return enabledMono.flatMapMany(
          enabled -> {
            if (!enabled) {
              return Flux.error(new FeatureFlagAccessDeniedException(featureName));
            }
            if (rollout < 100) {
              return Flux.deferContextual(
                  ctx -> {
                    ServerWebExchange exchange = ctx.get(ServerWebExchange.class);
                    Flux<Object> proceedFlux = proceedAsFlux(joinPoint);
                    return contextResolver
                        .resolve(exchange.getRequest())
                        .<Object>flatMapMany(
                            context -> {
                              if (!rolloutStrategy.isInRollout(featureName, context, rollout)) {
                                return Flux.error(
                                    new FeatureFlagAccessDeniedException(featureName));
                              }
                              return proceedFlux;
                            })
                        .switchIfEmpty(proceedFlux);
                  });
            }
            return proceedAsFlux(joinPoint);
          });
    }

    // Non-reactive return type: not supported in WebFlux
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

  public FeatureFlagAspect(
      ReactiveFeatureFlagProvider reactiveFeatureFlagProvider,
      RolloutStrategy rolloutStrategy,
      ReactiveFeatureFlagContextResolver contextResolver) {
    this.reactiveFeatureFlagProvider = reactiveFeatureFlagProvider;
    this.rolloutStrategy = rolloutStrategy;
    this.contextResolver = contextResolver;
  }
}
