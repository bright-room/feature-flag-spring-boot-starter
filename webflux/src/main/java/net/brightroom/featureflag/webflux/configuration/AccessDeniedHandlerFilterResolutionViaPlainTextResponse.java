package net.brightroom.featureflag.webflux.configuration;

import java.nio.charset.StandardCharsets;
import net.brightroom.featureflag.core.exception.FeatureFlagAccessDeniedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

class AccessDeniedHandlerFilterResolutionViaPlainTextResponse
    implements AccessDeniedHandlerFilterResolution {

  @Override
  public Mono<ServerResponse> resolve(
      @SuppressWarnings("unused") ServerRequest request, FeatureFlagAccessDeniedException e) {
    return ServerResponse.status(HttpStatus.FORBIDDEN)
        .contentType(new MediaType(MediaType.TEXT_PLAIN, StandardCharsets.UTF_8))
        .bodyValue(e.getMessage());
  }

  AccessDeniedHandlerFilterResolutionViaPlainTextResponse() {}
}
