package net.brightroom.featureflag.webmvc.exception;

import jakarta.servlet.http.HttpServletRequest;
import net.brightroom.featureflag.core.exception.FeatureFlagAccessDeniedException;
import net.brightroom.featureflag.webmvc.resolution.AccessDeniedInterceptResolution;
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
 * <p>If you want to guarantee that your {@code @ControllerAdvice} takes priority, annotate it with
 * an order value lower than {@code Ordered.LOWEST_PRECEDENCE} (e.g.,
 * {@code @Order(Ordered.LOWEST_PRECEDENCE - 1)} or simply {@code @Order(0)}).
 */
@ControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE)
public class FeatureFlagExceptionHandler {

  private final AccessDeniedInterceptResolution accessDeniedInterceptResolution;

  @ExceptionHandler(FeatureFlagAccessDeniedException.class)
  ResponseEntity<?> handleFeatureFlagAccessDenied(
      HttpServletRequest request, FeatureFlagAccessDeniedException e) {
    return accessDeniedInterceptResolution.resolution(request, e);
  }

  /**
   * Creates a new {@link FeatureFlagExceptionHandler} with the given resolution strategy.
   *
   * @param accessDeniedInterceptResolution the resolution to use when access is denied; must not be
   *     null
   */
  public FeatureFlagExceptionHandler(
      AccessDeniedInterceptResolution accessDeniedInterceptResolution) {
    this.accessDeniedInterceptResolution = accessDeniedInterceptResolution;
  }
}
