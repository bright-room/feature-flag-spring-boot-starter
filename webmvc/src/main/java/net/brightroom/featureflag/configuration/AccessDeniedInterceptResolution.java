package net.brightroom.featureflag.configuration;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** Interface for responses to a request that is denied access to a feature flag. */
public interface AccessDeniedInterceptResolution {
  void resolution(HttpServletRequest request, HttpServletResponse response);
}
