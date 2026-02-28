package net.brightroom.featureflag.webmvc.configuration;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Objects;
import net.brightroom.featureflag.core.annotation.FeatureFlag;
import net.brightroom.featureflag.core.exception.FeatureFlagAccessDeniedException;
import net.brightroom.featureflag.webmvc.provider.FeatureFlagProvider;
import org.jspecify.annotations.NonNull;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

class FeatureFlagInterceptor implements HandlerInterceptor {
  private final FeatureFlagProvider featureFlagProvider;

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
      return true;
    }

    FeatureFlag classAnnotation = handlerMethod.getBeanType().getAnnotation(FeatureFlag.class);
    validateAnnotation(classAnnotation);
    if (checkFeatureFlag(classAnnotation)) {
      throw new FeatureFlagAccessDeniedException(classAnnotation.value());
    }

    return true;
  }

  private void validateAnnotation(FeatureFlag annotation) {
    if (annotation != null && annotation.value().isEmpty()) {
      throw new IllegalStateException(
          "@FeatureFlag must specify a non-empty value. "
              + "An empty value causes fail-open behavior and allows access unconditionally.");
    }
  }

  private boolean checkFeatureFlag(FeatureFlag annotation) {
    return Objects.nonNull(annotation) && !featureFlagProvider.isFeatureEnabled(annotation.value());
  }

  FeatureFlagInterceptor(FeatureFlagProvider featureFlagProvider) {
    this.featureFlagProvider = featureFlagProvider;
  }
}
