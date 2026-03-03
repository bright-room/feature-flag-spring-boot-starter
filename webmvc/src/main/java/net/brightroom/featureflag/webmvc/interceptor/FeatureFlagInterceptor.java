package net.brightroom.featureflag.webmvc.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Optional;
import net.brightroom.featureflag.core.annotation.FeatureFlag;
import net.brightroom.featureflag.core.context.FeatureFlagContext;
import net.brightroom.featureflag.core.exception.FeatureFlagAccessDeniedException;
import net.brightroom.featureflag.core.provider.FeatureFlagProvider;
import net.brightroom.featureflag.core.rollout.RolloutStrategy;
import net.brightroom.featureflag.webmvc.context.FeatureFlagContextResolver;
import org.jspecify.annotations.NonNull;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

public class FeatureFlagInterceptor implements HandlerInterceptor {
  private final FeatureFlagProvider featureFlagProvider;
  private final RolloutStrategy rolloutStrategy;
  private final FeatureFlagContextResolver contextResolver;

  public FeatureFlagInterceptor(
      FeatureFlagProvider featureFlagProvider,
      RolloutStrategy rolloutStrategy,
      FeatureFlagContextResolver contextResolver) {
    this.featureFlagProvider = featureFlagProvider;
    this.rolloutStrategy = rolloutStrategy;
    this.contextResolver = contextResolver;
  }

  @Override
  public boolean preHandle(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull Object handler) {
    if (!(handler instanceof HandlerMethod handlerMethod)) {
      return true;
    }

    FeatureFlag methodAnnotation = handlerMethod.getMethodAnnotation(FeatureFlag.class);
    if (methodAnnotation != null) {
      validateAnnotation(methodAnnotation);
      if (checkFeatureFlag(methodAnnotation)) {
        throw new FeatureFlagAccessDeniedException(methodAnnotation.value());
      }
      checkRollout(request, methodAnnotation);
      return true;
    }

    FeatureFlag classAnnotation = handlerMethod.getBeanType().getAnnotation(FeatureFlag.class);
    if (classAnnotation == null) {
      return true;
    }
    validateAnnotation(classAnnotation);
    if (checkFeatureFlag(classAnnotation)) {
      throw new FeatureFlagAccessDeniedException(classAnnotation.value());
    }
    checkRollout(request, classAnnotation);

    return true;
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

  private boolean checkFeatureFlag(FeatureFlag annotation) {
    return !featureFlagProvider.isFeatureEnabled(annotation.value());
  }

  private void checkRollout(HttpServletRequest request, FeatureFlag annotation) {
    if (annotation.rollout() >= 100) return;
    Optional<FeatureFlagContext> context = contextResolver.resolve(request);
    if (context.isPresent()
        && !rolloutStrategy.isInRollout(annotation.value(), context.get(), annotation.rollout())) {
      throw new FeatureFlagAccessDeniedException(annotation.value());
    }
  }
}
