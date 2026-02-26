package net.brightroom.featureflag.webmvc.configuration;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.util.Map;
import net.brightroom.featureflag.core.exception.FeatureFlagAccessDeniedException;
import tools.jackson.databind.json.JsonMapper;

class AccessDeniedInterceptResolutionViaJsonResponse implements AccessDeniedInterceptResolution {

  JsonMapper jsonMapper;

  @Override
  public void resolution(
      HttpServletRequest request,
      HttpServletResponse response,
      FeatureFlagAccessDeniedException e) {
    response.setStatus(403);
    response.setContentType("application/json; charset=utf-8");

    Map<String, String> body =
        Map.of("error", "Feature flag access denied", "message", e.getMessage());

    try (PrintWriter writer = response.getWriter()) {
      String json = jsonMapper.writeValueAsString(body);
      writer.write(json);
    } catch (Exception ex) {
      throw new IllegalStateException("Response json conversion failed", ex);
    }
  }

  AccessDeniedInterceptResolutionViaJsonResponse(JsonMapper jsonMapper) {
    this.jsonMapper = jsonMapper;
  }
}
