package net.brightroom.featureflag.webflux.resolution.handlerfilter;

import java.nio.charset.StandardCharsets;
import net.brightroom.featureflag.core.exception.FeatureFlagAccessDeniedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.util.HtmlUtils;
import reactor.core.publisher.Mono;

class AccessDeniedHandlerFilterResolutionViaHtmlResponse
    implements AccessDeniedHandlerFilterResolution {

  private static final MediaType TEXT_HTML_UTF8 =
      new MediaType(MediaType.TEXT_HTML, StandardCharsets.UTF_8);

  /**
   * {@inheritDoc}
   *
   * <p>Note: the {@code request} parameter is not used in this implementation.
   */
  @Override
  public Mono<ServerResponse> resolve(ServerRequest request, FeatureFlagAccessDeniedException e) {
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
    return ServerResponse.status(HttpStatus.FORBIDDEN).contentType(TEXT_HTML_UTF8).bodyValue(html);
  }

  AccessDeniedHandlerFilterResolutionViaHtmlResponse() {}
}
