package net.brightroom.featureflag.webflux.configuration;

import net.brightroom.featureflag.core.configuration.FeatureFlagProperties;
import net.brightroom.featureflag.core.configuration.ResponseProperties;
import tools.jackson.databind.json.JsonMapper;

class AccessDeniedReactiveResolutionFactory {

  private final JsonMapper objectMapper;

  AccessDeniedReactiveResolution create(FeatureFlagProperties featureFlagProperties) {
    ResponseProperties responseProperties = featureFlagProperties.response();

    return switch (responseProperties.type()) {
      case PLAIN_TEXT -> new AccessDeniedReactiveResolutionViaPlainTextResponse();
      case HTML -> new AccessDeniedReactiveResolutionViaHtmlResponse();
      case JSON -> new AccessDeniedReactiveResolutionViaJsonResponse(objectMapper);
    };
  }

  AccessDeniedReactiveResolutionFactory(JsonMapper objectMapper) {
    this.objectMapper = objectMapper;
  }
}
