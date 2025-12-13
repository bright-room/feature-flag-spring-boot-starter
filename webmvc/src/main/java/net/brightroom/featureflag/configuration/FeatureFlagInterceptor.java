package net.brightroom.featureflag.configuration;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;
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

    boolean isAccessDenied =
        extractFeatureFlags(handlerMethod).stream()
            .map(FeatureFlag::value)
            .anyMatch(featureName -> !featureFlagProvider.isFeatureEnabled(featureName));

    if (isAccessDenied) {
      throw new FeatureFlagAccessDeniedException("Feature flag is disabled.");
    }

    return true;
  }

  private List<FeatureFlag> extractFeatureFlags(HandlerMethod handlerMethod) {
    List<FeatureFlag> featureFlags = new ArrayList<>();

    FeatureFlag methodAnnotation = handlerMethod.getMethodAnnotation(FeatureFlag.class);
    if (Objects.nonNull(methodAnnotation)) featureFlags.add(methodAnnotation);

    FeatureFlag classAnnotation = handlerMethod.getBeanType().getAnnotation(FeatureFlag.class);
    if (Objects.nonNull(classAnnotation)) featureFlags.add(classAnnotation);

    return featureFlags;
  }

  FeatureFlagInterceptor(FeatureFlagProvider featureFlagProvider) {
    this.featureFlagProvider = featureFlagProvider;
  }
}
