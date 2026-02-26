package net.brightroom.featureflag.webmvc.configuration;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.net.URI;
import net.brightroom.featureflag.core.exception.FeatureFlagAccessDeniedException;
import org.springframework.http.ProblemDetail;
import tools.jackson.databind.json.JsonMapper;

class AccessDeniedInterceptResolutionViaRFC7807JsonResponse
    implements AccessDeniedInterceptResolution {

  JsonMapper jsonMapper;

  @Override
  public void resolution(
      HttpServletRequest request,
      HttpServletResponse response,
      FeatureFlagAccessDeniedException e) {
    int statusCode = 403;

    response.setStatus(statusCode);
    response.setContentType("application/problem+json; charset=utf-8");

    ProblemDetail problemDetail = ProblemDetail.forStatus(statusCode);
    problemDetail.setType(
        URI.create("https://github.com/bright-room/feature-flag-spring-boot-starter"));
    problemDetail.setTitle("Feature flag access denied");
    problemDetail.setDetail(e.getMessage());
    problemDetail.setInstance(URI.create(request.getRequestURI()));

    try (PrintWriter writer = response.getWriter()) {
      String json = jsonMapper.writeValueAsString(problemDetail);
      writer.write(json);
    } catch (Exception ex) {
      throw new IllegalStateException("Response json conversion failed", ex);
    }
  }

  AccessDeniedInterceptResolutionViaRFC7807JsonResponse(JsonMapper jsonMapper) {
    this.jsonMapper = jsonMapper;
  }
}
