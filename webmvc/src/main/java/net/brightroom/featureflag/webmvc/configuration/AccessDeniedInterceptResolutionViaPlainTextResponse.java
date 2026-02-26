package net.brightroom.featureflag.webmvc.configuration;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import net.brightroom.featureflag.core.exception.FeatureFlagAccessDeniedException;

class AccessDeniedInterceptResolutionViaPlainTextResponse
    implements AccessDeniedInterceptResolution {

  @Override
  public void resolution(
      HttpServletRequest request,
      HttpServletResponse response,
      FeatureFlagAccessDeniedException e) {
    response.setStatus(403);
    response.setContentType("text/plain; charset=utf-8");

    try (PrintWriter writer = response.getWriter()) {
      writer.write(e.getMessage());
    } catch (Exception ex) {
      throw new IllegalStateException("Response text conversion failed", ex);
    }
  }

  AccessDeniedInterceptResolutionViaPlainTextResponse() {}
}
