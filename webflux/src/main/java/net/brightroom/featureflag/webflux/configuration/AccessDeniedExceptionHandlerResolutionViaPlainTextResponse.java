package net.brightroom.featureflag.webflux.configuration;

import java.nio.charset.StandardCharsets;
import net.brightroom.featureflag.core.exception.FeatureFlagAccessDeniedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;

class AccessDeniedExceptionHandlerResolutionViaPlainTextResponse
    implements AccessDeniedExceptionHandlerResolution {

  private static final MediaType TEXT_PLAIN_UTF8 =
      new MediaType(MediaType.TEXT_PLAIN, StandardCharsets.UTF_8);

  @Override
  public ResponseEntity<?> resolution(
      @SuppressWarnings("unused") ServerHttpRequest request, FeatureFlagAccessDeniedException e) {
    return ResponseEntity.status(HttpStatus.FORBIDDEN)
        .contentType(TEXT_PLAIN_UTF8)
        .body(e.getMessage());
  }

  AccessDeniedExceptionHandlerResolutionViaPlainTextResponse() {}
}
