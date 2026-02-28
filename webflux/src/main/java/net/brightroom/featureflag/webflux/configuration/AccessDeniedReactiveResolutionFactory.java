package net.brightroom.featureflag.webflux.configuration;

import net.brightroom.featureflag.core.configuration.FeatureFlagProperties;
import net.brightroom.featureflag.core.configuration.ResponseProperties;
import tools.jackson.databind.ObjectMapper;

class AccessDeniedReactiveResolutionFactory {

  private final ObjectMapper objectMapper;

  AccessDeniedReactiveResolution create(FeatureFlagProperties featureFlagProperties) {
    ResponseProperties responseProperties = featureFlagProperties.response();

    return switch (responseProperties.type()) {
      case PLAIN_TEXT -> new AccessDeniedReactiveResolutionViaPlainTextResponse();
      case HTML -> new AccessDeniedReactiveResolutionViaHtmlResponse();
      case JSON -> new AccessDeniedReactiveResolutionViaJsonResponse(objectMapper);
    };
  }

  AccessDeniedReactiveResolutionFactory(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }
}
