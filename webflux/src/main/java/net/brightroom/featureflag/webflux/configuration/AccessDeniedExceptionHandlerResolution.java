package net.brightroom.featureflag.webflux.configuration;

import net.brightroom.featureflag.core.exception.FeatureFlagAccessDeniedException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;

/**
 * Interface for handling cases where access to a feature flag protected resource is denied in a
 * {@link org.springframework.web.bind.annotation.ControllerAdvice} context.
 *
 * <p>Implementations return a {@link ResponseEntity} which is written through Spring's response
 * processing pipeline, enabling content negotiation and standard message converters.
 */
interface AccessDeniedExceptionHandlerResolution {

  /**
   * Resolves the response when access to a feature flag protected resource is denied.
   *
   * @param request the reactive HTTP request that was denied access
   * @param e the FeatureFlagAccessDeniedException that triggered the resolution
   * @return a ResponseEntity representing the denial response
   */
  ResponseEntity<?> resolution(ServerHttpRequest request, FeatureFlagAccessDeniedException e);
}
