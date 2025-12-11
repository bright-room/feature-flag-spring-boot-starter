package net.brightroom.featureflag.configuration;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.ModelAndView;

/** Interface for responses to a request that is denied access to a feature flag. */
public interface FeatureFlagAccessDeniedResponse {

  /**
   * Writes the response to the given HttpServletResponse.
   *
   * @param response the HttpServletResponse to write to
   */
  default void writeTo(HttpServletResponse response) {}

  /**
   * Returns a ModelAndView that can be used to render the response.
   *
   * @return the ModelAndView to render
   */
  default ModelAndView toModelAndView() {
    return new ModelAndView();
  }
}
