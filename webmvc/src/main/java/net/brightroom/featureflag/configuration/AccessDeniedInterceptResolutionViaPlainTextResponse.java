package net.brightroom.featureflag.configuration;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.PrintWriter;

class AccessDeniedInterceptResolutionViaPlainTextResponse
    implements AccessDeniedInterceptResolution {
  int statusCode;
  String message;

  AccessDeniedInterceptResolutionViaPlainTextResponse(int statusCode, String message) {
    this.statusCode = statusCode;
    this.message = message;
  }

  @Override
  public void resolution(HttpServletRequest request, HttpServletResponse response) {
    response.setStatus(statusCode);
    response.setContentType("text/plain; charset=utf-8");

    try (PrintWriter writer = response.getWriter()) {
      writer.write(message);
    } catch (Exception e) {
      throw new IllegalStateException("Response text conversion failed", e);
    }
  }

  AccessDeniedInterceptResolutionViaPlainTextResponse() {}
}
