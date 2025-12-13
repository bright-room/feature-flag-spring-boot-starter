package net.brightroom.featureflag.configuration;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Interface for handling cases where access to a feature flag protected resource is denied.
 *
 * <p>The {@link AccessDeniedInterceptResolution} interface serves as a strategy to determine how
 * HTTP requests should be resolved when access is restricted due to a disabled feature flag.
 * Implementations of this interface are used in conjunction with the {@link FeatureFlagInterceptor}
 * to provide customized responses for denied access scenarios.
 *
 * <p>Implementations of this interface can define various resolutions such as returning JSON
 * responses, redirecting to a specific view, providing RFC 7807-compliant error responses, or plain
 * text messages, depending on the application's requirements.
 */
public interface AccessDeniedInterceptResolution {

  /**
   * Resolves the response when access to a feature flag protected resource is denied.
   *
   * <p>This method is called by the {@link FeatureFlagInterceptor} when a request attempts to
   * access a resource protected by a feature flag that is disabled. Implementations should handle
   * the HTTP response appropriately, such as setting status codes, content types, and response
   * bodies.
   *
   * @param request the HTTP servlet request that was denied access
   * @param response the HTTP servlet response to be configured for the denial
   */
  void resolution(HttpServletRequest request, HttpServletResponse response);
}
