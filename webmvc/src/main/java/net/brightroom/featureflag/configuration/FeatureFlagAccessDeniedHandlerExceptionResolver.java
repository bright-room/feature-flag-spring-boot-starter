package net.brightroom.featureflag.configuration;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.brightroom.featureflag.exception.FeatureFlagAccessDeniedException;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;

class FeatureFlagAccessDeniedHandlerExceptionResolver implements HandlerExceptionResolver {

  FeatureFlagAccessDeniedResponse featureFlagAccessDeniedResponse;

  @Override
  public ModelAndView resolveException(
      HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
    if (ex instanceof FeatureFlagAccessDeniedException) {
      featureFlagAccessDeniedResponse.writeTo(response);
      return featureFlagAccessDeniedResponse.toModelAndView();
    }

    return null;
  }

  FeatureFlagAccessDeniedHandlerExceptionResolver(
      FeatureFlagAccessDeniedResponse featureFlagAccessDeniedResponse) {
    this.featureFlagAccessDeniedResponse = featureFlagAccessDeniedResponse;
  }
}
