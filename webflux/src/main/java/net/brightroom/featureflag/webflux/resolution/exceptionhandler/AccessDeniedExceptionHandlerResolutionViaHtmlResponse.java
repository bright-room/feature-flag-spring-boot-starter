package net.brightroom.featureflag.webflux.resolution.exceptionhandler;

import java.nio.charset.StandardCharsets;
import net.brightroom.featureflag.core.exception.FeatureFlagAccessDeniedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.util.HtmlUtils;

class AccessDeniedExceptionHandlerResolutionViaHtmlResponse
    implements AccessDeniedExceptionHandlerResolution {

  private static final MediaType TEXT_HTML_UTF8 =
      new MediaType(MediaType.TEXT_HTML, StandardCharsets.UTF_8);

  @Override
  public ResponseEntity<?> resolution(
      @SuppressWarnings("unused") ServerHttpRequest request, FeatureFlagAccessDeniedException e) {
    String escapedMessage = HtmlUtils.htmlEscape(e.getMessage());
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

    return ResponseEntity.status(HttpStatus.FORBIDDEN).contentType(TEXT_HTML_UTF8).body(html);
  }

  AccessDeniedExceptionHandlerResolutionViaHtmlResponse() {}
}
