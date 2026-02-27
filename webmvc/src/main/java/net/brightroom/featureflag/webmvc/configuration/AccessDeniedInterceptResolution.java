package net.brightroom.featureflag.webmvc.configuration;

import jakarta.servlet.http.HttpServletRequest;
import net.brightroom.featureflag.core.exception.FeatureFlagAccessDeniedException;
import org.springframework.http.ResponseEntity;

/**
 * Interface for handling cases where access to a feature flag protected resource is denied.
 *
 * <p>The {@link AccessDeniedInterceptResolution} interface serves as a strategy to determine how
 * HTTP requests should be resolved when access is restricted due to a disabled feature flag.
 * Implementations of this interface are used in conjunction with the {@link FeatureFlagInterceptor}
 * to provide customized responses for denied access scenarios.
 *
 * <p>Implementations of this interface can define various resolutions such as returning JSON,
 * plain text, or HTML responses, depending on the application's requirements.
 */
interface AccessDeniedInterceptResolution {

  /**
   * Resolves the response when access to a feature flag protected resource is denied.
   *
   * <p>This method is called by {@link FeatureFlagExceptionHandler} when {@link
   * FeatureFlagAccessDeniedException} is thrown by the interceptor. Implementations return a {@link
   * ResponseEntity} which is written through Spring's response processing pipeline, enabling
   * content negotiation and standard message converters.
   *
   * @param request the HTTP servlet request that was denied access
   * @param e the FeatureFlagAccessDeniedException that triggered the resolution
   * @return a ResponseEntity representing the denial response
   */
  ResponseEntity<?> resolution(HttpServletRequest request, FeatureFlagAccessDeniedException e);
}
