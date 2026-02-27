package net.brightroom.featureflag.webmvc.configuration;

import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import net.brightroom.featureflag.core.exception.FeatureFlagAccessDeniedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

class AccessDeniedInterceptResolutionViaJsonResponse implements AccessDeniedInterceptResolution {

  @Override
  public ResponseEntity<?> resolution(
      HttpServletRequest request, FeatureFlagAccessDeniedException e) {
    Map<String, String> body = new LinkedHashMap<>();
    body.put("error", "Feature flag access denied");
    body.put("message", e.getMessage());

    return ResponseEntity.status(HttpStatus.FORBIDDEN)
        .contentType(MediaType.APPLICATION_JSON)
        .body(body);
  }

  AccessDeniedInterceptResolutionViaJsonResponse() {}
}
