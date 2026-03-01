package net.brightroom.featureflag.core.resolution;

import net.brightroom.featureflag.core.exception.FeatureFlagAccessDeniedException;
import org.springframework.web.util.HtmlUtils;

/** Utility for building HTML 403 responses for feature flag access denial. */
public final class HtmlResponseBuilder {

  private HtmlResponseBuilder() {}

  /**
   * Builds an HTML body for a denied feature flag access.
   *
   * @param e the exception that triggered the denial
   * @return an HTML string with status 403
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
}
