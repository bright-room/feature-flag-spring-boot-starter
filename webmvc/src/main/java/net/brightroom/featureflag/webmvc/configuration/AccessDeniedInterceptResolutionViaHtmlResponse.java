package net.brightroom.featureflag.webmvc.configuration;

import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import net.brightroom.featureflag.core.exception.FeatureFlagAccessDeniedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

class AccessDeniedInterceptResolutionViaHtmlResponse implements AccessDeniedInterceptResolution {

  @Override
  public ResponseEntity<?> resolution(
      HttpServletRequest request, FeatureFlagAccessDeniedException e) {
    String escapedMessage =
        e.getMessage()
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;");

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

    return ResponseEntity.status(HttpStatus.FORBIDDEN)
        .contentType(new MediaType(MediaType.TEXT_HTML, StandardCharsets.UTF_8))
        .body(html);
  }

  AccessDeniedInterceptResolutionViaHtmlResponse() {}
}
