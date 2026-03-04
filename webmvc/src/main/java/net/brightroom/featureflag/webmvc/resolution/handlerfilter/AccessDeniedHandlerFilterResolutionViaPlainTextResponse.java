package net.brightroom.featureflag.webmvc.resolution.handlerfilter;

import java.nio.charset.StandardCharsets;
import net.brightroom.featureflag.core.exception.FeatureFlagAccessDeniedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

class AccessDeniedHandlerFilterResolutionViaPlainTextResponse
    implements AccessDeniedHandlerFilterResolution {

  private static final MediaType TEXT_PLAIN_UTF8 =
      new MediaType(MediaType.TEXT_PLAIN, StandardCharsets.UTF_8);

  /**
   * {@inheritDoc}
   *
   * <p>Note: the {@code request} parameter is not used in this implementation.
   */
  @Override
  public ServerResponse resolve(ServerRequest request, FeatureFlagAccessDeniedException e) {
    return ServerResponse.status(HttpStatus.FORBIDDEN)
        .contentType(TEXT_PLAIN_UTF8)
        .body(e.getMessage());
  }

  AccessDeniedHandlerFilterResolutionViaPlainTextResponse() {}
}
