package net.brightroom.featureflag.core.resolution;

import net.brightroom.featureflag.core.exception.FeatureFlagAccessDeniedException;
import org.springframework.web.util.HtmlUtils;

/** Utility class for building an HTML response body for feature flag access denials. */
public final class HtmlResponseBuilder {

  /**
   * Builds an HTML response body for a feature flag access denial.
   *
   * @param e the exception that triggered the denial
   * @return an HTML string with a 403 Access Denied page
   */
  public static String buildHtml(FeatureFlagAccessDeniedException e) {
    String escapedMessage = HtmlUtils.htmlEscape(e.getMessage());
    return """
        <!DOCTYPE html>
        <html lang="en">
        <head>
          <meta charset="UTF-8">
          <title>Access Denied</title>
        </head>
        <body>
          <h1>403 - Access Denied</h1>
          <p>%s</p>
        </body>
        </html>
        """
        .formatted(escapedMessage);
  }

  private HtmlResponseBuilder() {}
}
