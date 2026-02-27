package net.brightroom.featureflag.webmvc.configuration;

import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import net.brightroom.featureflag.core.exception.FeatureFlagAccessDeniedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

/**
 * An {@link AccessDeniedInterceptResolution} implementation that returns a fixed HTML response.
 *
 * <p>This implementation returns a {@code ResponseEntity<String>} with {@code text/html} content
 * type, which Spring MVC writes using {@code StringHttpMessageConverter}. The converter can only
 * write the response when the client's {@code Accept} header includes {@code text/html} or {@code
 * text/*}. If the client sends {@code Accept: application/json} only, Spring MVC will return {@code
 * 406 Not Acceptable} instead of the intended HTML response.
 *
 * <p>For full control over the denied response regardless of the {@code Accept} header, define a
 * custom {@code @ControllerAdvice} that handles {@link
 * net.brightroom.featureflag.core.exception.FeatureFlagAccessDeniedException}.
 */
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
