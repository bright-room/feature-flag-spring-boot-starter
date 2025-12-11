package net.brightroom.featureflag.configuration;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

class FeatureFlagAccessDeniedPlainTextResponse implements FeatureFlagAccessDeniedResponse {
  int statusCode;
  String contentType = "text/plain";
  String message;

  FeatureFlagAccessDeniedPlainTextResponse(int statusCode, String message) {
    this.statusCode = statusCode;
    this.message = message;
  }

  @Override
  public void writeTo(HttpServletResponse response) {
    try (PrintWriter writer = response.getWriter()) {
      response.setStatus(statusCode);
      response.setContentType(contentType);

      writer.print(message);
      writer.flush();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  FeatureFlagAccessDeniedPlainTextResponse() {}
}
