package net.brightroom.featureflag.webflux.configuration;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import net.brightroom.featureflag.core.exception.FeatureFlagAccessDeniedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.codec.HttpMessageWriter;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.reactive.result.view.ViewResolver;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

class AccessDeniedWebFilterResolutionViaJsonResponse implements AccessDeniedWebFilterResolution {

  private final ServerResponse.Context context;

  @Override
  public Mono<Void> resolve(ServerWebExchange exchange, FeatureFlagAccessDeniedException e) {
    ProblemDetail problemDetail = buildProblemDetail(exchange, e);
    return ServerResponse.status(HttpStatus.FORBIDDEN)
        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
        .bodyValue(problemDetail)
        .flatMap(serverResponse -> serverResponse.writeTo(exchange, context));
  }

  private ProblemDetail buildProblemDetail(
      ServerWebExchange exchange, FeatureFlagAccessDeniedException e) {
    ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.FORBIDDEN);
    problemDetail.setType(
        URI.create(
            "https://github.com/bright-room/feature-flag-spring-boot-starter#response-types"));
    problemDetail.setTitle("Feature flag access denied");
    problemDetail.setDetail(e.getMessage());
    problemDetail.setInstance(URI.create(exchange.getRequest().getPath().value()));
    return problemDetail;
  }

  AccessDeniedWebFilterResolutionViaJsonResponse(ServerCodecConfigurer codecConfigurer) {
    this.context = new CodecContext(codecConfigurer.getWriters());
  }

  private record CodecContext(List<HttpMessageWriter<?>> messageWriters)
      implements ServerResponse.Context {
    @Override
    public List<ViewResolver> viewResolvers() {
      return Collections.emptyList();
    }
  }
}
