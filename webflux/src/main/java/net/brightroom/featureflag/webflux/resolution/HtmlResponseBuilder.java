package net.brightroom.featureflag.webflux.resolution;

import net.brightroom.featureflag.core.exception.FeatureFlagAccessDeniedException;
import org.springframework.web.util.HtmlUtils;

public class HtmlResponseBuilder {

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
