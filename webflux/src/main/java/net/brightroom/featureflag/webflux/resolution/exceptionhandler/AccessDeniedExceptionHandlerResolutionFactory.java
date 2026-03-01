package net.brightroom.featureflag.webflux.resolution.exceptionhandler;

import net.brightroom.featureflag.core.properties.FeatureFlagProperties;
import net.brightroom.featureflag.core.properties.ResponseProperties;

public class AccessDeniedExceptionHandlerResolutionFactory {

  public AccessDeniedExceptionHandlerResolution create(
      FeatureFlagProperties featureFlagProperties) {
    ResponseProperties responseProperties = featureFlagProperties.response();

    return switch (responseProperties.type()) {
      case PLAIN_TEXT -> new AccessDeniedExceptionHandlerResolutionViaPlainTextResponse();
      case HTML -> new AccessDeniedExceptionHandlerResolutionViaHtmlResponse();
      case JSON -> new AccessDeniedExceptionHandlerResolutionViaJsonResponse();
    };
  }

  public AccessDeniedExceptionHandlerResolutionFactory() {}
}
