package net.brightroom.featureflag.configuration;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.util.Map;
import tools.jackson.databind.json.JsonMapper;

class AccessDeniedInterceptResolutionViaJsonResponse implements AccessDeniedInterceptResolution {
  int statusCode;
  Map<String, String> body;

  JsonMapper jsonMapper;

  AccessDeniedInterceptResolutionViaJsonResponse(
      int statusCode, Map<String, String> body, JsonMapper jsonMapper) {
    this.statusCode = statusCode;
    this.body = body;
    this.jsonMapper = jsonMapper;
  }

  @Override
  public void resolution(HttpServletRequest request, HttpServletResponse response) {
    response.setStatus(statusCode);
    response.setContentType("application/json; charset=utf-8");

    try (PrintWriter writer = response.getWriter()) {
      String json = jsonMapper.writeValueAsString(body);
      writer.write(json);
    } catch (Exception e) {
      throw new IllegalStateException("Response json conversion failed", e);
    }
  }

  AccessDeniedInterceptResolutionViaJsonResponse() {}
}
