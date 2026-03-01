package net.brightroom.featureflag.webmvc.resolution;

import jakarta.servlet.http.HttpServletRequest;
import net.brightroom.featureflag.core.exception.FeatureFlagAccessDeniedException;
import net.brightroom.featureflag.core.resolution.ProblemDetailBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

class AccessDeniedInterceptResolutionViaJsonResponse implements AccessDeniedInterceptResolution {

  @Override
  public ResponseEntity<?> resolution(
      HttpServletRequest request, FeatureFlagAccessDeniedException e) {
    var body = ProblemDetailBuilder.build(request.getRequestURI(), e);
    return ResponseEntity.status(HttpStatus.FORBIDDEN)
        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
        .body(body);
  }

  AccessDeniedInterceptResolutionViaJsonResponse() {}
}
