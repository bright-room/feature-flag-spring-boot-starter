package net.brightroom.featureflag.configuration;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import org.springframework.web.servlet.ModelAndView;

class FeatureFlagAccessDeniedPlainTextResponse implements FeatureFlagAccessDeniedResponse {
  Integer statusCode;
  String contentType = "text/plain";
  String message;

  FeatureFlagAccessDeniedPlainTextResponse(Integer statusCode, String message) {
    this.statusCode = statusCode;
    this.message = message;
  }

  @Override
  public void writeTo(HttpServletResponse response) {
    try {
      response.setStatus(statusCode);

      PrintWriter writer = response.getWriter();
      writer.print(message);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public ModelAndView toModelAndView() {
    return new ModelAndView();
  }

  FeatureFlagAccessDeniedPlainTextResponse() {}
}
