package net.brightroom.featureflag.webflux.configuration;

import java.nio.charset.StandardCharsets;
import net.brightroom.featureflag.core.exception.FeatureFlagAccessDeniedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

class AccessDeniedHandlerFilterResolutionViaHtmlResponse
    implements AccessDeniedHandlerFilterResolution {

  /**
   * {@inheritDoc}
   *
   * <p>Note: the {@code request} parameter is not used in this implementation.
   */
  @Override
  public Mono<ServerResponse> resolve(ServerRequest request, FeatureFlagAccessDeniedException e) {
    String html = HtmlResponseBuilder.buildHtml(e);
    return ServerResponse.status(HttpStatus.FORBIDDEN)
        .contentType(new MediaType(MediaType.TEXT_HTML, StandardCharsets.UTF_8))
        .bodyValue(html);
  }

  AccessDeniedHandlerFilterResolutionViaHtmlResponse() {}
}
