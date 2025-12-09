package net.brightroom.featureflag.configuration;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Objects;
import net.brightroom.featureflag.annotation.FeatureFlag;
import net.brightroom.featureflag.exception.FeatureFlagAccessDeniedException;
import net.brightroom.featureflag.provider.FeatureFlagProvider;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

class FeatureFlagInterceptor implements HandlerInterceptor {
  FeatureFlagProvider featureFlagProvider;

  @Override
  public boolean preHandle(
      HttpServletRequest request, HttpServletResponse response, Object handler) {
    if (!(handler instanceof HandlerMethod handlerMethod)) {
      return true;
    }

    FeatureFlag methodAnnotation = handlerMethod.getMethodAnnotation(FeatureFlag.class);
    if (checkFeatureFlag(methodAnnotation))
      throw new FeatureFlagAccessDeniedException("Feature flag is disabled.");

    FeatureFlag classAnnotation = handlerMethod.getBeanType().getAnnotation(FeatureFlag.class);
    if (checkFeatureFlag(classAnnotation))
      throw new FeatureFlagAccessDeniedException("Feature flag is disabled.");

    return true;
  }

  private boolean checkFeatureFlag(FeatureFlag annotation) {
    return Objects.nonNull(annotation) && !featureFlagProvider.isFeatureEnabled(annotation.value());
  }

  FeatureFlagInterceptor(FeatureFlagProvider featureFlagProvider) {
    this.featureFlagProvider = featureFlagProvider;
  }
}
