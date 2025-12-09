package net.brightroom.featureflag.configuration;

import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.json.JacksonJsonView;

class FeatureFlagAccessDeniedJsonResponse implements FeatureFlagAccessDeniedResponse {
  Integer statusCode;
  String contentType = "application/json";
  Map<String, String> body;

  FeatureFlagAccessDeniedJsonResponse(Integer statusCode, Map<String, String> body) {
    this.statusCode = statusCode;
    this.body = body;
  }

  @Override
  public void writeTo(HttpServletResponse response) {}

  @Override
  public ModelAndView toModelAndView() {
    JacksonJsonView o = new JacksonJsonView();
    o.setAttributesMap(body);

    ModelAndView modelAndView = new ModelAndView(o);
    modelAndView.setStatus(HttpStatusCode.valueOf(statusCode));

    return modelAndView;
  }

  FeatureFlagAccessDeniedJsonResponse() {}
}
