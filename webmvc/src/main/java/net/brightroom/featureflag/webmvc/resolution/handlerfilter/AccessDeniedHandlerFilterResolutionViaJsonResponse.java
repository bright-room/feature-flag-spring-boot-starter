package net.brightroom.featureflag.webmvc.resolution.handlerfilter;

import net.brightroom.featureflag.core.exception.FeatureFlagAccessDeniedException;
import net.brightroom.featureflag.core.resolution.ProblemDetailBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

class AccessDeniedHandlerFilterResolutionViaJsonResponse
    implements AccessDeniedHandlerFilterResolution {

  @Override
  public ServerResponse resolve(ServerRequest request, FeatureFlagAccessDeniedException e) {
    var body = ProblemDetailBuilder.build(request.path(), e);
    return ServerResponse.status(HttpStatus.FORBIDDEN)
        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
        .body(body);
  }

  AccessDeniedHandlerFilterResolutionViaJsonResponse() {}
}
