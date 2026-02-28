package net.brightroom.featureflag.webflux.configuration;

import java.net.URI;
import net.brightroom.featureflag.core.exception.FeatureFlagAccessDeniedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;

class AccessDeniedExceptionHandlerResolutionViaJsonResponse
    implements AccessDeniedExceptionHandlerResolution {

  @Override
  public ResponseEntity<?> resolution(
      ServerHttpRequest request, FeatureFlagAccessDeniedException e) {
    ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.FORBIDDEN);
    problemDetail.setType(
        URI.create(
            "https://github.com/bright-room/feature-flag-spring-boot-starter#response-types"));
    problemDetail.setTitle("Feature flag access denied");
    problemDetail.setDetail(e.getMessage());
    problemDetail.setInstance(URI.create(request.getPath().value()));

    return ResponseEntity.status(HttpStatus.FORBIDDEN)
        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
        .body(problemDetail);
  }

  AccessDeniedExceptionHandlerResolutionViaJsonResponse() {}
}
