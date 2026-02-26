package net.brightroom.featureflag.webmvc.configuration;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.net.URI;
import java.util.Map;
import org.springframework.http.ProblemDetail;
import tools.jackson.databind.json.JsonMapper;

class AccessDeniedInterceptResolutionViaRFC7807JsonResponse
    implements AccessDeniedInterceptResolution {
  int statusCode;
  Map<String, String> body;

  JsonMapper jsonMapper;

  AccessDeniedInterceptResolutionViaRFC7807JsonResponse(
      int statusCode, Map<String, String> body, JsonMapper jsonMapper) {
    this.statusCode = statusCode;
    this.body = body;
    this.jsonMapper = jsonMapper;
  }

  @Override
  public void resolution(HttpServletRequest request, HttpServletResponse response) {
    response.setStatus(statusCode);
    response.setContentType("application/problem+json; charset=utf-8");

    try (PrintWriter writer = response.getWriter()) {
      ProblemDetail problemDetail = ProblemDetail.forStatus(statusCode);
      problemDetail.setType(
          URI.create("https://github.com/bright-room/feature-flag-spring-boot-starter"));
      problemDetail.setTitle("Access Denied");
      problemDetail.setDetail("The feature flag is disabled");
      problemDetail.setInstance(URI.create(request.getRequestURI()));
      for (String key : body.keySet()) {
        problemDetail.setProperty(key, body.getOrDefault(key, ""));
      }

      String json = jsonMapper.writeValueAsString(problemDetail);
      writer.write(json);
    } catch (Exception e) {
      throw new IllegalStateException("Response json conversion failed", e);
    }
  }

  AccessDeniedInterceptResolutionViaRFC7807JsonResponse() {}
}
