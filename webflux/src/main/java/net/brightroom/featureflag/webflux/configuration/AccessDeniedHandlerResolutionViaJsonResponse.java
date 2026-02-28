package net.brightroom.featureflag.webflux.configuration;

import java.net.URI;
import net.brightroom.featureflag.core.exception.FeatureFlagAccessDeniedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

class AccessDeniedHandlerResolutionViaJsonResponse implements AccessDeniedHandlerResolution {

  @Override
  public Mono<ServerResponse> resolve(ServerRequest request, FeatureFlagAccessDeniedException e) {
    ProblemDetail problemDetail = buildProblemDetail(request, e);
    return ServerResponse.status(HttpStatus.FORBIDDEN)
        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
        .bodyValue(problemDetail);
  }

  private ProblemDetail buildProblemDetail(
      ServerRequest request, FeatureFlagAccessDeniedException e) {
    ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.FORBIDDEN);
    problemDetail.setType(
        URI.create(
            "https://github.com/bright-room/feature-flag-spring-boot-starter#response-types"));
    problemDetail.setTitle("Feature flag access denied");
    problemDetail.setDetail(e.getMessage());
    problemDetail.setInstance(URI.create(request.path()));
    return problemDetail;
  }

  AccessDeniedHandlerResolutionViaJsonResponse() {}
}
