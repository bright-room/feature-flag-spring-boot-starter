package net.brightroom.featureflag.webmvc.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import net.brightroom.featureflag.core.annotation.FeatureFlag;
import net.brightroom.featureflag.core.context.FeatureFlagContext;
import net.brightroom.featureflag.core.exception.FeatureFlagAccessDeniedException;
import net.brightroom.featureflag.core.provider.FeatureFlagProvider;
import net.brightroom.featureflag.webmvc.context.FeatureFlagContextResolver;
import org.jspecify.annotations.NonNull;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

public class FeatureFlagInterceptor implements HandlerInterceptor {
  private final FeatureFlagProvider featureFlagProvider;
  private final FeatureFlagContextResolver contextResolver;

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
      if (checkFeatureFlag(methodAnnotation, request)) {
        throw new FeatureFlagAccessDeniedException(methodAnnotation.value());
      }
      return true;
    }

    FeatureFlag classAnnotation = handlerMethod.getBeanType().getAnnotation(FeatureFlag.class);
    validateAnnotation(classAnnotation);
    if (checkFeatureFlag(classAnnotation, request)) {
      throw new FeatureFlagAccessDeniedException(classAnnotation.value());
    }

    return true;
  }

  private void validateAnnotation(FeatureFlag annotation) {
    if (annotation == null) {
      return;
    }
    if (annotation.value().isEmpty()) {
      throw new IllegalStateException(
          "@FeatureFlag must specify a non-empty value. "
              + "An empty value causes fail-open behavior and allows access unconditionally.");
    }
    int rollout = annotation.rollout();
    if (rollout < 0 || rollout > 100) {
      throw new IllegalStateException(
          "@FeatureFlag rollout must be between 0 and 100, but was: " + rollout);
    }
  }

  private boolean checkFeatureFlag(FeatureFlag annotation, HttpServletRequest request) {
    if (annotation == null) {
      return false;
    }

    if (!featureFlagProvider.isFeatureEnabled(annotation.value())) {
      return true;
    }

    int rollout = annotation.rollout();
    if (rollout >= 100) {
      return false;
    }
    if (rollout <= 0) {
      return true;
    }

    FeatureFlagContext context = contextResolver.resolve(request);
    int bucket = computeBucket(annotation.value(), context.userId());
    return bucket >= rollout;
  }

  private int computeBucket(String featureName, String userId) {
    try {
      MessageDigest md = MessageDigest.getInstance("SHA-256");
      byte[] hash = md.digest((featureName + ":" + userId).getBytes(StandardCharsets.UTF_8));
      int value =
          ((hash[0] & 0xFF) << 24)
              | ((hash[1] & 0xFF) << 16)
              | ((hash[2] & 0xFF) << 8)
              | (hash[3] & 0xFF);
      return Integer.remainderUnsigned(value, 100);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 is not available", e);
    }
  }

  public FeatureFlagInterceptor(
      FeatureFlagProvider featureFlagProvider, FeatureFlagContextResolver contextResolver) {
    this.featureFlagProvider = featureFlagProvider;
    this.contextResolver = contextResolver;
  }
}
