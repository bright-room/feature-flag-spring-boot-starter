package net.brightroom.featureflag.webmvc.configuration;

import jakarta.servlet.http.HttpServletRequest;
import net.brightroom.featureflag.core.exception.FeatureFlagAccessDeniedException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * Default handler for {@link FeatureFlagAccessDeniedException}.
 *
 * <p>This handler has the lowest precedence ({@code @Order(Ordered.LOWEST_PRECEDENCE)}), so any
 * user-defined {@code @ControllerAdvice} handling the same exception will take priority.
 *
 * <p>If you want to guarantee that your {@code @ControllerAdvice} takes priority, specify an
 * explicit order value (e.g., {@code @Order(1)}).
 */
@ControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE)
class FeatureFlagExceptionHandler {

  private final AccessDeniedInterceptResolution accessDeniedInterceptResolution;

  @ExceptionHandler(FeatureFlagAccessDeniedException.class)
  ResponseEntity<?> handleFeatureFlagAccessDenied(
      HttpServletRequest request, FeatureFlagAccessDeniedException e) {
    return accessDeniedInterceptResolution.resolution(request, e);
  }

  FeatureFlagExceptionHandler(AccessDeniedInterceptResolution accessDeniedInterceptResolution) {
    this.accessDeniedInterceptResolution = accessDeniedInterceptResolution;
  }
}
