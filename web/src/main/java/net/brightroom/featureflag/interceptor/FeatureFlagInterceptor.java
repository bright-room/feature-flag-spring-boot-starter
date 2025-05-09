package net.brightroom.featureflag.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Objects;
import net.brightroom.featureflag.annotation.FeatureFlag;
import net.brightroom.featureflag.provider.FeatureFlagProvider;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Spring MVC interceptor for Feature Flag checking. This interceptor handles feature flag checks
 * for MVC endpoints by intercepting requests before they reach the handlers.
 */
public class FeatureFlagInterceptor implements HandlerInterceptor {

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
    if (Objects.nonNull(methodAnnotation) && checkFeatureFlag(methodAnnotation)) {
      writeResponse(response);
      return false;
    }

    FeatureFlag classAnnotation = handlerMethod.getBeanType().getAnnotation(FeatureFlag.class);
    if (Objects.nonNull(classAnnotation) && checkFeatureFlag(classAnnotation)) {
      writeResponse(response);
      return false;
    }
    return true;
  }

  private boolean checkFeatureFlag(FeatureFlag annotation) {
    return !featureFlagProvider.isFeatureEnabled(annotation.feature());
  }

  private void writeResponse(HttpServletResponse response) {
    try {
      response.setStatus(HttpStatus.METHOD_NOT_ALLOWED.value());

      PrintWriter writer = response.getWriter();
      writer.write("This feature is not available");
    } catch (IOException e) {
      throw new RuntimeException("Fail to write response.", e);
    }
  }

  /**
   * Constructor
   *
   * @param featureFlagProvider featureFlagProvider
   */
  public FeatureFlagInterceptor(FeatureFlagProvider featureFlagProvider) {
    this.featureFlagProvider = featureFlagProvider;
  }
}
