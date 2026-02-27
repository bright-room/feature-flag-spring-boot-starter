package net.brightroom.featureflag.webmvc.configuration;

import net.brightroom.featureflag.core.configuration.FeatureFlagProperties;
import net.brightroom.featureflag.core.configuration.ResponseProperties;

class AccessDeniedInterceptResolutionFactory {

  AccessDeniedInterceptResolution create(
      FeatureFlagProperties featureFlagProperties, boolean useRFC7807) {
    ResponseProperties responseProperties = featureFlagProperties.response();

    return switch (responseProperties.type()) {
      case PLAIN_TEXT -> new AccessDeniedInterceptResolutionViaPlainTextResponse();
      case HTML -> new AccessDeniedInterceptResolutionViaHtmlResponse();
      case JSON -> {
        if (useRFC7807) yield new AccessDeniedInterceptResolutionViaRFC7807JsonResponse();
        else yield new AccessDeniedInterceptResolutionViaJsonResponse();
      }
    };
  }

  AccessDeniedInterceptResolutionFactory() {}
}
