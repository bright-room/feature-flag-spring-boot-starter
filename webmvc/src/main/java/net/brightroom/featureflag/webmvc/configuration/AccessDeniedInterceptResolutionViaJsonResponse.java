package net.brightroom.featureflag.webmvc.configuration;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import net.brightroom.featureflag.core.exception.FeatureFlagAccessDeniedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;

class AccessDeniedInterceptResolutionViaJsonResponse implements AccessDeniedInterceptResolution {

  @Override
  public ResponseEntity<?> resolution(
      HttpServletRequest request, FeatureFlagAccessDeniedException e) {
    ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.FORBIDDEN);
    problemDetail.setType(
        URI.create("https://github.com/bright-room/feature-flag-spring-boot-starter"));
    problemDetail.setTitle("Feature flag access denied");
    problemDetail.setDetail(e.getMessage());
    problemDetail.setInstance(URI.create(request.getRequestURI()));

    return ResponseEntity.status(HttpStatus.FORBIDDEN)
        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
        .body(problemDetail);
  }

  AccessDeniedInterceptResolutionViaJsonResponse() {}
}
