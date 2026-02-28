package net.brightroom.featureflag.webflux.configuration;

import java.lang.reflect.Method;
import net.brightroom.featureflag.core.annotation.FeatureFlag;
import net.brightroom.featureflag.core.exception.FeatureFlagAccessDeniedException;
import net.brightroom.featureflag.webflux.provider.ReactiveFeatureFlagProvider;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.aop.support.AopUtils;
import org.springframework.core.annotation.AnnotationUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Aspect
class FeatureFlagAspect {

  private final ReactiveFeatureFlagProvider reactiveFeatureFlagProvider;

  @Around(
      "@within(net.brightroom.featureflag.core.annotation.FeatureFlag) || "
          + "@annotation(net.brightroom.featureflag.core.annotation.FeatureFlag)")
  Object checkFeatureFlag(ProceedingJoinPoint joinPoint) throws Throwable {
    FeatureFlag annotation = resolveAnnotation(joinPoint);
    if (annotation == null) {
      return joinPoint.proceed();
    }

    validateAnnotation(annotation);

    String featureName = annotation.value();
    Mono<Boolean> enabledMono = reactiveFeatureFlagProvider.isFeatureEnabled(featureName);

    Class<?> returnType = ((MethodSignature) joinPoint.getSignature()).getReturnType();

    if (Mono.class.isAssignableFrom(returnType)) {
      return enabledMono.flatMap(
          enabled -> {
            if (!enabled) {
              return Mono.error(new FeatureFlagAccessDeniedException(featureName));
            }
            try {
              return (Mono<?>) joinPoint.proceed();
            } catch (Throwable t) {
              return Mono.error(t);
            }
          });
    }

    if (Flux.class.isAssignableFrom(returnType)) {
      return enabledMono.flatMapMany(
          enabled -> {
            if (!enabled) {
              return Flux.error(new FeatureFlagAccessDeniedException(featureName));
            }
            try {
              return (Flux<?>) joinPoint.proceed();
            } catch (Throwable t) {
              return Flux.error(t);
            }
          });
    }

    // Non-reactive return type: not supported in WebFlux
    throw new IllegalStateException(
        "@FeatureFlag on WebFlux controller method '"
            + ((MethodSignature) joinPoint.getSignature()).getMethod().getName()
            + "' requires a reactive return type (Mono or Flux). "
            + "Non-reactive return types are not supported.");
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
  }

  FeatureFlagAspect(ReactiveFeatureFlagProvider reactiveFeatureFlagProvider) {
    this.reactiveFeatureFlagProvider = reactiveFeatureFlagProvider;
  }
}
