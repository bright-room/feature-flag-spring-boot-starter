package net.brightroom.featureflag.webflux.configuration;

import net.brightroom.featureflag.core.configuration.FeatureFlagProperties;
import net.brightroom.featureflag.core.configuration.ResponseProperties;

class AccessDeniedExceptionHandlerResolutionFactory {

  AccessDeniedExceptionHandlerResolution create(FeatureFlagProperties featureFlagProperties) {
    ResponseProperties responseProperties = featureFlagProperties.response();

    return switch (responseProperties.type()) {
      case PLAIN_TEXT -> new AccessDeniedExceptionHandlerResolutionViaPlainTextResponse();
      case HTML -> new AccessDeniedExceptionHandlerResolutionViaHtmlResponse();
      case JSON -> new AccessDeniedExceptionHandlerResolutionViaJsonResponse();
    };
  }

  AccessDeniedExceptionHandlerResolutionFactory() {}
}
