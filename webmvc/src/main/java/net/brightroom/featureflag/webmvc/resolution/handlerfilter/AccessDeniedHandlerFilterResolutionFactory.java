package net.brightroom.featureflag.webmvc.resolution.handlerfilter;

import net.brightroom.featureflag.core.properties.FeatureFlagProperties;
import net.brightroom.featureflag.core.properties.ResponseProperties;

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
