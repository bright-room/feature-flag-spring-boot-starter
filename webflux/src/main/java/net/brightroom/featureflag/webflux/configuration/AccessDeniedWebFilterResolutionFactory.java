package net.brightroom.featureflag.webflux.configuration;

import net.brightroom.featureflag.core.configuration.FeatureFlagProperties;
import net.brightroom.featureflag.core.configuration.ResponseProperties;
import org.springframework.http.codec.ServerCodecConfigurer;

class AccessDeniedWebFilterResolutionFactory {

  private final ServerCodecConfigurer codecConfigurer;

  AccessDeniedWebFilterResolution create(FeatureFlagProperties featureFlagProperties) {
    ResponseProperties responseProperties = featureFlagProperties.response();

    return switch (responseProperties.type()) {
      case PLAIN_TEXT -> new AccessDeniedWebFilterResolutionViaPlainTextResponse();
      case HTML -> new AccessDeniedWebFilterResolutionViaHtmlResponse();
      case JSON -> new AccessDeniedWebFilterResolutionViaJsonResponse(codecConfigurer);
    };
  }

  AccessDeniedWebFilterResolutionFactory(ServerCodecConfigurer codecConfigurer) {
    this.codecConfigurer = codecConfigurer;
  }
}
