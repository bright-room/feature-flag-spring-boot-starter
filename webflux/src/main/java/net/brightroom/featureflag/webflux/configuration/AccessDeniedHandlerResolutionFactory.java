package net.brightroom.featureflag.webflux.configuration;

import net.brightroom.featureflag.core.configuration.FeatureFlagProperties;
import net.brightroom.featureflag.core.configuration.ResponseProperties;

class AccessDeniedHandlerResolutionFactory {

  AccessDeniedHandlerResolution create(FeatureFlagProperties featureFlagProperties) {
    ResponseProperties responseProperties = featureFlagProperties.response();

    return switch (responseProperties.type()) {
      case PLAIN_TEXT -> new AccessDeniedHandlerResolutionViaPlainTextResponse();
      case HTML -> new AccessDeniedHandlerResolutionViaHtmlResponse();
      case JSON -> new AccessDeniedHandlerResolutionViaJsonResponse();
    };
  }

  AccessDeniedHandlerResolutionFactory() {}
}
