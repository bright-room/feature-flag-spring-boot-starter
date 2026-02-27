package net.brightroom.featureflag.webmvc.configuration;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.brightroom.featureflag.core.exception.FeatureFlagAccessDeniedException;

class AccessDeniedInterceptResolutionViaHtmlResponse implements AccessDeniedInterceptResolution {

  @Override
  public void resolution(
      HttpServletRequest request,
      HttpServletResponse response,
      FeatureFlagAccessDeniedException e) {
    response.setStatus(403);
    response.setContentType("text/html; charset=utf-8");

    String escapedMessage =
        e.getMessage()
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;");

    String html =
        """
        <!DOCTYPE html>
        <html lang="en">
        <head>
          <meta charset="UTF-8">
          <title>Access Denied</title>
        </head>
        <body>
          <h1>403 - Access Denied</h1>
          <p>%s</p>
        </body>
        </html>
        """
            .formatted(escapedMessage);

    try (var writer = response.getWriter()) {
      writer.write(html);
    } catch (Exception ex) {
      throw new IllegalStateException("Response write failed", ex);
    }
  }

  AccessDeniedInterceptResolutionViaHtmlResponse() {}
}
