package net.brightroom.featureflag.configuration;

import java.util.Map;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.json.JacksonJsonView;

class FeatureFlagAccessDeniedJsonResponse implements FeatureFlagAccessDeniedResponse {
  int statusCode;
  Map<String, String> body;

  FeatureFlagAccessDeniedJsonResponse(int statusCode, Map<String, String> body) {
    this.statusCode = statusCode;
    this.body = body;
  }

  @Override
  public ModelAndView toModelAndView() {
    JacksonJsonView jsonView = new JacksonJsonView();
    jsonView.setAttributesMap(body);

    ModelAndView modelAndView = new ModelAndView(jsonView);
    modelAndView.setStatus(HttpStatusCode.valueOf(statusCode));

    return modelAndView;
  }

  FeatureFlagAccessDeniedJsonResponse() {}
}
