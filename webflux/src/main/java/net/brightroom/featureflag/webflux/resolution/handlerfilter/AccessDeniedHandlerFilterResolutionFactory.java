package net.brightroom.featureflag.webflux.resolution.handlerfilter;

import net.brightroom.featureflag.core.configuration.FeatureFlagProperties;
import net.brightroom.featureflag.core.configuration.ResponseProperties;

public class AccessDeniedHandlerFilterResolutionFactory {

  public AccessDeniedHandlerFilterResolution create(FeatureFlagProperties featureFlagProperties) {
    ResponseProperties responseProperties = featureFlagProperties.response();

    return switch (responseProperties.type()) {
      case PLAIN_TEXT -> new AccessDeniedHandlerFilterResolutionViaPlainTextResponse();
      case HTML -> new AccessDeniedHandlerFilterResolutionViaHtmlResponse();
      case JSON -> new AccessDeniedHandlerFilterResolutionViaJsonResponse();
    };
  }

  public AccessDeniedHandlerFilterResolutionFactory() {}
}
