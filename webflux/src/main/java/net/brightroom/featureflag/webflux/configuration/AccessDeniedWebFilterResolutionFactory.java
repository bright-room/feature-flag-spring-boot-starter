package net.brightroom.featureflag.webflux.configuration;

import net.brightroom.featureflag.core.configuration.FeatureFlagProperties;
import net.brightroom.featureflag.core.configuration.ResponseProperties;
import tools.jackson.databind.json.JsonMapper;

class AccessDeniedWebFilterResolutionFactory {

  private final JsonMapper objectMapper;

  AccessDeniedWebFilterResolution create(FeatureFlagProperties featureFlagProperties) {
    ResponseProperties responseProperties = featureFlagProperties.response();

    return switch (responseProperties.type()) {
      case PLAIN_TEXT -> new AccessDeniedWebFilterResolutionViaPlainTextResponse();
      case HTML -> new AccessDeniedWebFilterResolutionViaHtmlResponse();
      case JSON -> new AccessDeniedWebFilterResolutionViaJsonResponse(objectMapper);
    };
  }

  AccessDeniedWebFilterResolutionFactory(JsonMapper objectMapper) {
    this.objectMapper = objectMapper;
  }
}
