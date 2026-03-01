package net.brightroom.featureflag.webmvc.resolution;

import net.brightroom.featureflag.core.configuration.FeatureFlagProperties;
import net.brightroom.featureflag.core.configuration.ResponseProperties;

public class AccessDeniedInterceptResolutionFactory {

  public AccessDeniedInterceptResolution create(FeatureFlagProperties featureFlagProperties) {
    ResponseProperties responseProperties = featureFlagProperties.response();

    return switch (responseProperties.type()) {
      case PLAIN_TEXT -> new AccessDeniedInterceptResolutionViaPlainTextResponse();
      case HTML -> new AccessDeniedInterceptResolutionViaHtmlResponse();
      case JSON -> new AccessDeniedInterceptResolutionViaJsonResponse();
    };
  }

  public AccessDeniedInterceptResolutionFactory() {}
}
