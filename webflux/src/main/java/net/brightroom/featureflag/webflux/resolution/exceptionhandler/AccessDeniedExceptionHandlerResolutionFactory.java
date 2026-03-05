package net.brightroom.featureflag.webflux.resolution.exceptionhandler;

import net.brightroom.featureflag.core.properties.FeatureFlagProperties;
import net.brightroom.featureflag.core.properties.ResponseProperties;

/**
 * Factory for creating {@link AccessDeniedExceptionHandlerResolution} instances.
 *
 * <p>Selects the appropriate resolution implementation based on the configured {@code
 * feature-flags.response.type} property.
 */
public class AccessDeniedExceptionHandlerResolutionFactory {

  /**
   * Creates an {@link AccessDeniedExceptionHandlerResolution} based on the response type configured
   * in the given {@link FeatureFlagProperties}.
   *
   * @param featureFlagProperties the feature flag configuration properties
   * @return the resolution implementation matching the configured response type
   */
  public AccessDeniedExceptionHandlerResolution create(
      FeatureFlagProperties featureFlagProperties) {
    ResponseProperties responseProperties = featureFlagProperties.response();

    return switch (responseProperties.type()) {
      case PLAIN_TEXT -> new AccessDeniedExceptionHandlerResolutionViaPlainTextResponse();
      case HTML -> new AccessDeniedExceptionHandlerResolutionViaHtmlResponse();
      case JSON -> new AccessDeniedExceptionHandlerResolutionViaJsonResponse();
    };
  }

  /** Creates a new {@code AccessDeniedExceptionHandlerResolutionFactory}. */
  public AccessDeniedExceptionHandlerResolutionFactory() {}
}
