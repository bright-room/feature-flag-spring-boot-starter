package net.brightroom.featureflag.webflux.configuration;

import java.net.URI;
import net.brightroom.featureflag.core.exception.FeatureFlagAccessDeniedException;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

class AccessDeniedReactiveResolutionViaJsonResponse implements AccessDeniedReactiveResolution {

  private final ObjectMapper objectMapper;

  @Override
  public Mono<Void> resolve(ServerWebExchange exchange, FeatureFlagAccessDeniedException e) {
    ServerHttpResponse response = exchange.getResponse();
    response.setStatusCode(HttpStatus.FORBIDDEN);
    response.getHeaders().setContentType(MediaType.APPLICATION_PROBLEM_JSON);

    ProblemDetail problemDetail = buildProblemDetail(exchange, e);
    return writeBody(response, problemDetail);
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

  private Mono<Void> writeBody(ServerHttpResponse response, ProblemDetail problemDetail) {
    try {
      byte[] body = objectMapper.writeValueAsBytes(problemDetail);
      DataBuffer buffer = response.bufferFactory().wrap(body);
      return response.writeWith(Mono.just(buffer));
    } catch (JacksonException ex) {
      return Mono.error(ex);
    }
  }

  AccessDeniedReactiveResolutionViaJsonResponse(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }
}
