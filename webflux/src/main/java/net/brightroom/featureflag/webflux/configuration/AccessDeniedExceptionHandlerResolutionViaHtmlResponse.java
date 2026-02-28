package net.brightroom.featureflag.webflux.configuration;

import java.nio.charset.StandardCharsets;
import net.brightroom.featureflag.core.exception.FeatureFlagAccessDeniedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;

class AccessDeniedExceptionHandlerResolutionViaHtmlResponse
    implements AccessDeniedExceptionHandlerResolution {

  @Override
  public ResponseEntity<?> resolution(
      @SuppressWarnings("unused") ServerHttpRequest request, FeatureFlagAccessDeniedException e) {
    String html = HtmlResponseBuilder.buildHtml(e);

    return ResponseEntity.status(HttpStatus.FORBIDDEN)
        .contentType(new MediaType(MediaType.TEXT_HTML, StandardCharsets.UTF_8))
        .body(html);
  }

  AccessDeniedExceptionHandlerResolutionViaHtmlResponse() {}
}
