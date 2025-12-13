package net.brightroom.featureflag.configuration;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;

class AccessDeniedInterceptResolutionViaViewResponse implements AccessDeniedInterceptResolution {
  int statusCode;
  String uri;
  Map<String, String> attributes;

  AccessDeniedInterceptResolutionViaViewResponse(
      int statusCode, String uri, Map<String, String> attributes) {
    this.statusCode = statusCode;
    this.uri = uri;
    this.attributes = attributes;
  }

  @Override
  public void resolution(HttpServletRequest request, HttpServletResponse response) {
    response.setStatus(statusCode);

    for (Map.Entry<String, String> entry : attributes.entrySet()) {
      request.setAttribute(entry.getKey(), entry.getValue());
    }

    RequestDispatcher dispatcher = request.getRequestDispatcher(uri);
    try {
      dispatcher.forward(request, response);
    } catch (Exception e) {
      throw new IllegalStateException("Request dispatcher failed", e);
    }
  }

  AccessDeniedInterceptResolutionViaViewResponse() {}
}
