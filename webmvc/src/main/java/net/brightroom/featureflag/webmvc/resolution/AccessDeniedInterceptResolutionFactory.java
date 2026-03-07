package net.brightroom.featureflag.webmvc.resolution;

import net.brightroom.featureflag.core.properties.FeatureFlagProperties;
import net.brightroom.featureflag.core.properties.ResponseProperties;

/**
 * Factory for creating {@link AccessDeniedInterceptResolution} instances based on the configured
 * response type.
 *
 * <p>Selects the appropriate resolution implementation from {@link
 * net.brightroom.featureflag.core.properties.ResponseType ResponseType} configured via {@code
 * feature-flags.response.type}.
 */
public class AccessDeniedInterceptResolutionFactory {

  /**
   * Creates an {@link AccessDeniedInterceptResolution} appropriate for the response type configured
   * in the given properties.
   *
   * @param featureFlagProperties the feature flag configuration properties; must not be null
   * @return the resolution implementation matching the configured response type
   */
  public AccessDeniedInterceptResolution create(FeatureFlagProperties featureFlagProperties) {
    ResponseProperties responseProperties = featureFlagProperties.response();

    return switch (responseProperties.type()) {
      case PLAIN_TEXT -> new AccessDeniedInterceptResolutionViaPlainTextResponse();
      case HTML -> new AccessDeniedInterceptResolutionViaHtmlResponse();
      case JSON -> new AccessDeniedInterceptResolutionViaJsonResponse();
    };
  }

  /** Creates a new {@link AccessDeniedInterceptResolutionFactory}. */
  public AccessDeniedInterceptResolutionFactory() {}
}
